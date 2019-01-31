/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.vr.sdk.samples.hellovr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;

import com.google.common.logging.nano.Vr;
import com.google.vr.sdk.audio.GvrAudioEngine;
import com.google.vr.sdk.base.AndroidCompat;
import com.google.vr.sdk.base.Eye;
import com.google.vr.sdk.base.GvrActivity;
import com.google.vr.sdk.base.GvrView;
import com.google.vr.sdk.base.HeadTransform;
import com.google.vr.sdk.base.Viewport;
import com.google.vr.sdk.base.sensors.internal.Matrix3x3d;
import com.google.vr.sdk.base.sensors.internal.Vector3d;
import com.google.vr.sdk.samples.hellovr.TCP.TcpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Random;
import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Google VR sample application.
 *
 * <p>This app presents a scene consisting of a room and a floating object. When the user finds the
 * object, they can invoke the trigger action, and a new object will be randomly spawned. When in
 * Cardboard mode, the user must gaze at the object and use the Cardboard trigger button. When in
 * Daydream mode, the user can use the controller to position the cursor, and use the controller
 * buttons to invoke the trigger action.
 */
public class HelloVrActivity extends GvrActivity implements GvrView.StereoRenderer {
  private static final String TAG = "HelloVrActivity";

  private static final int TARGET_MESH_COUNT = 3;

  private static final float Z_NEAR = 0.01f;
  private static final float Z_FAR = 10.0f;

  // Convenience vector for extracting the position from a matrix via multiplication.
  private static final float[] POS_MATRIX_MULTIPLY_VEC = {0.0f, 0.0f, 0.0f, 1.0f};
  private static final float[] FORWARD_VEC = {0.0f, 0.0f, -1.0f, 1.f};

  private static final float MIN_TARGET_DISTANCE = 3.0f;
  private static final float MAX_TARGET_DISTANCE = 3.5f;

  private static final String OBJECT_SOUND_FILE = "audio/HelloVR_Loop.ogg";
  private static final String SUCCESS_SOUND_FILE = "audio/HelloVR_Activation.ogg";

  private static final float FLOOR_HEIGHT = -2.0f;

  private static final float ANGLE_LIMIT = 0.2f;

  // The maximum yaw and pitch of the target object, in degrees. After hiding the target, its
  // yaw will be within [-MAX_YAW, MAX_YAW] and pitch will be within [-MAX_PITCH, MAX_PITCH].
  private static final float MAX_YAW = 100.0f;
  private static final float MAX_PITCH = 25.0f;

  private static final String[] OBJECT_VERTEX_SHADER_CODE =
      new String[] {
        "uniform mat4 u_MVP;",
        "attribute vec4 a_Position;",
        "attribute vec2 a_UV;",
        "varying vec2 v_UV;",
        "",
        "void main() {",
        "  v_UV = a_UV;",
        //"  gl_Position = u_MVP * a_Position;",
        "  gl_Position = a_Position;",
        "}",
      };
  private static final String[] OBJECT_FRAGMENT_SHADER_CODE =
      new String[] {
        "precision mediump float;",
        "varying vec2 v_UV;",
        "uniform sampler2D u_Texture;",
        "",
        "void main() {",
        "  // The y coordinate of this sample's textures is reversed compared to",
        "  // what OpenGL expects, so we invert the y coordinate.",
        "  gl_FragColor = texture2D(u_Texture, vec2(v_UV.x, 1.0 - v_UV.y));",
        "}",
      };

  private int objectProgram;

  private int objectPositionParam;
  private int objectUvParam;
  private int objectModelViewProjectionParam;

  private float targetDistance = MAX_TARGET_DISTANCE;

  private TexturedMesh room;
  private Texture roomTex;
  private ArrayList<TexturedMesh> targetObjectMeshes;

  private TexturedMesh leftEye;
  private TexturedMesh rightEye;

  private Texture leftTex;
  private Texture rightTex;

  //private ArrayList<Texture> targetObjectNotSelectedTextures;
  //private ArrayList<Texture> targetObjectSelectedTextures;
  private int curTargetObject;

  private Random random;

  private float[] targetPosition;
  private float[] camera;
  private float[] view;
  private float[] headView;
  private float[] modelViewProjection;
  private float[] modelView;

  private float[] modelTarget;
  private float[] modelRoom;

  private float[] tempPosition;
  private float[] headRotation;

  private GvrAudioEngine gvrAudioEngine;
  private volatile int sourceId = GvrAudioEngine.INVALID_ID;
  private volatile int successSourceId = GvrAudioEngine.INVALID_ID;

  //tututututu
  TcpClient mTcpClient;

  /**
   * Sets the view to our GvrView and initializes the transformation matrices we will use
   * to render our scene.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    StrictMode.ThreadPolicy old = StrictMode.getThreadPolicy();
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .permitDiskWrites()
            .build());
    //doCorrectStuffThatWritesToDisk();
    //StrictMode.setThreadPolicy(old);
    ////tututututu
    square = new Square();

    initializeGvrView();

    camera = new float[16];
    view = new float[16];
    modelViewProjection = new float[16];
    modelView = new float[16];
    // Target object first appears directly in front of user.
    targetPosition = new float[] {0.0f, 0.0f, -MIN_TARGET_DISTANCE};
    tempPosition = new float[4];
    //headRotation = new float[4];
    modelTarget = new float[16];
    modelRoom = new float[16];
    headView = new float[16];

    // Initialize 3D audio engine.
    gvrAudioEngine = new GvrAudioEngine(this, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY);

    random = new Random();
    //tutututu
    //String URL = "http://192.168.1.37:5000/video_feed";
    //String URL = "http://192.168.1.39:5000/video_feed";
    //new DoRead().execute(URL);
    //setSource(MjpegInputStream.read(URL));
    //thread = new MjpegViewThread();
    //new DoRead().execute(URL);
    AppSettings.ReadFromSharedPreferences(getApplicationContext());

    view1 = findViewById(R.id.mjpegview1);
    view1.setAdjustHeight(true);
    view1.setMode(MjpegView.MODE_FIT_WIDTH);
    //view.setMsecWaitAfterReadImageError(1000);
    //view1.setUrl("http://bma-itic1.iticfoundation.org/mjpeg2.php?camid=61.91.182.114:1111");
    view1.setUrl(AppSettings.MJPEGstreamURL);
    view1.setRecycleBitmap(true);

    new ConnectTask().execute("");
  }
  private MjpegView view1;

  public class ConnectTask extends AsyncTask<String, String, TcpClient> {

    @Override
    protected TcpClient doInBackground(String... message) {

      //we create a TCPClient object
      mTcpClient = new TcpClient(new TcpClient.OnMessageReceived() {
        @Override
        //here the messageReceived method is implemented
        public void messageReceived(String message) {
          //this method calls the onProgressUpdate
          publishProgress(message);
        }
      });
      mTcpClient.run();

      return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
      super.onProgressUpdate(values);
      //response received from server
      if (values[0].compareTo("done") == 0) {
        mTcpClient.CurrentTimeMillis = System.currentTimeMillis();
        mTcpClient.Block = false;
      }
        //mTcpClient.CurrentTimeMillis = System.currentTimeMillis() + 500;
      Log.d("test", "response " + values[0]);
      //process server response here....

    }
  }
  public void initializeGvrView() {
    setContentView(R.layout.common_ui);

    GvrView gvrView = (GvrView) findViewById(R.id.gvr_view);
    gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8);

    gvrView.setRenderer(this);
    gvrView.setTransitionViewEnabled(true);

    // Enable Cardboard-trigger feedback with Daydream headsets. This is a simple way of supporting
    // Daydream controller input for basic interactions using the existing Cardboard trigger API.
    gvrView.enableCardboardTriggerEmulation();

    if (gvrView.setAsyncReprojectionEnabled(true)) {
      // Async reprojection decouples the app framerate from the display framerate,
      // allowing immersive interaction even at the throttled clockrates set by
      // sustained performance mode.
      AndroidCompat.setSustainedPerformanceMode(this, true);
    }

    setGvrView(gvrView);
  }

  @Override
  public void onPause() {
    gvrAudioEngine.pause();
    view1.stopStream();
    super.onPause();

    if (mTcpClient != null) {
      mTcpClient.stopClient();
    }
  }

  @Override
  protected void onStop() {
    view1.stopStream();
    super.onStop();

    if (mTcpClient != null) {
      mTcpClient.stopClient();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    gvrAudioEngine.resume();
    view1.startStream();
  }

  @Override
  public void onRendererShutdown() {
    Log.i(TAG, "onRendererShutdown");
  }

  int width = 0;
  int height = 0;
  @Override
  public void onSurfaceChanged(int width, int height) {
    this.width = width;
    this.height = height;

    Log.i(TAG, "onSurfaceChanged");
  }

  /**
   * Creates the buffers we use to store information about the 3D world.
   *
   * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
   * Hence we use ByteBuffers.
   *
   * @param config The EGL configuration used when creating the surface.
   */
  @Override
  public void onSurfaceCreated(EGLConfig config) {
    Log.i(TAG, "onSurfaceCreated");
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

    objectProgram = Util.compileProgram(OBJECT_VERTEX_SHADER_CODE, OBJECT_FRAGMENT_SHADER_CODE);

    objectPositionParam = GLES20.glGetAttribLocation(objectProgram, "a_Position");
    objectUvParam = GLES20.glGetAttribLocation(objectProgram, "a_UV");
    objectModelViewProjectionParam = GLES20.glGetUniformLocation(objectProgram, "u_MVP");

    Util.checkGlError("Object program params");

    Matrix.setIdentityM(modelRoom, 0);
    Matrix.translateM(modelRoom, 0, 0, FLOOR_HEIGHT, 0);

    // Avoid any delays during start-up due to decoding of sound files.
    new Thread(
            new Runnable() {
              @Override
              public void run() {
                // Start spatial audio playback of OBJECT_SOUND_FILE at the model position. The
                // returned sourceId handle is stored and allows for repositioning the sound object
                // whenever the target position changes.
                gvrAudioEngine.preloadSoundFile(OBJECT_SOUND_FILE);
                sourceId = gvrAudioEngine.createSoundObject(OBJECT_SOUND_FILE);
                gvrAudioEngine.setSoundObjectPosition(
                    sourceId, targetPosition[0], targetPosition[1], targetPosition[2]);
                gvrAudioEngine.playSound(sourceId, true /* looped playback */);
                // Preload an unspatialized sound to be played on a successful trigger on the
                // target.
                gvrAudioEngine.preloadSoundFile(SUCCESS_SOUND_FILE);
              }
            })
        .start();

    updateTargetPosition();

    Util.checkGlError("onSurfaceCreated");

    try {
      room = new TexturedMesh(this, "CubeRoom.obj", objectPositionParam, objectUvParam);
      roomTex = new Texture(this, "CubeRoom_BakedDiffuse.png");
      targetObjectMeshes = new ArrayList<>();
      //targetObjectNotSelectedTextures = new ArrayList<>();
      //targetObjectSelectedTextures = new ArrayList<>();
      targetObjectMeshes.add(
          new TexturedMesh(this, "square.obj", objectPositionParam, objectUvParam));
      //targetObjectNotSelectedTextures.add(new Texture(this, "tex.png"));
      //targetObjectSelectedTextures.add(new Texture(this, "tex.png"));
      targetObjectMeshes.add(
          new TexturedMesh(this, "square.obj", objectPositionParam, objectUvParam));
      //targetObjectNotSelectedTextures.add(new Texture(this, "tex.png"));
      //targetObjectSelectedTextures.add(new Texture(this, "tex.png"));
      targetObjectMeshes.add(
          new TexturedMesh(this, "square.obj", objectPositionParam, objectUvParam));
      //targetObjectNotSelectedTextures.add(new Texture(this, "tex.png"));

      leftEye = new TexturedMesh(this, "square.obj", objectPositionParam, objectUvParam);
      rightEye = new TexturedMesh(this, "square.obj", objectPositionParam, objectUvParam);

      leftTex = new Texture(this, "texL.png");
      rightTex = new Texture(this, "texR.png");

      textureBitmap = BitmapFactory.decodeStream(getAssets().open("tex.png"));
      textureBitmap = textureBitmap.copy( textureBitmap.getConfig() , true);
      Bitmap.Config bc = textureBitmap.getConfig();
      int a = 0;
      a++;
      //targetObjectSelectedTextures.add(new Texture(this, "tex.png"));
    } catch (IOException e) {
      Log.e(TAG, "Unable to initialize objects", e);
    }
    curTargetObject = random.nextInt(TARGET_MESH_COUNT);
  }

  /** Updates the target object position. */
  private void updateTargetPosition() {
    Matrix.setIdentityM(modelTarget, 0);
    Matrix.translateM(modelTarget, 0, targetPosition[0], targetPosition[1], targetPosition[2]);

    // Update the sound location to match it with the new target position.
    if (sourceId != GvrAudioEngine.INVALID_ID) {
      gvrAudioEngine.setSoundObjectPosition(
          sourceId, targetPosition[0], targetPosition[1], targetPosition[2]);
    }
    Util.checkGlError("updateTargetPosition");
  }

  //Vector3d baseHeadPosition = null;
  Object synHeadPositionClick = new Object();
  MotorWiFiControl.Command prevCommand = MotorWiFiControl.Command.Stop;
  Vector3d Vector3dY = new Vector3d(0, 1, 0);
  Vector3d Vector3dX = new Vector3d(-1, 0, 0);

  private double calculateAngle(Vector3d a, Vector3d b)
  {
    double cos = Vector3d.dot(a, b) / (a.length() * b.length());
    return Math.acos(cos);
  }

  private double calculateAngle(Vector3d a, Vector3d b, Vector3d up)
  {
    double cos = Vector3d.dot(a, b) / (a.length() * b.length());
    Vector3d upn = new Vector3d(up.x, up.y, up.z);
    Vector3d cross = new Vector3d();
    Vector3d.cross(upn, a, cross);
    double sin = Vector3d.dot(cross, b) / (a.length() * b.length());
    double angle = 0;
    if (sin > 0)
    {
      angle = Math.acos(cos);
    }
    else
    {
      angle = 2 * Math.PI - Math.acos(cos);
    }
    if (angle > Math.PI)
      angle -= 2 * Math.PI;
    return angle;
  }

  Quaternion baseQuaternion = null;
  /**
   * Prepares OpenGL ES before we draw a frame.
   *
   * @param headTransform The head transformation in the new frame.
   */
  @Override
  public void onNewFrame(HeadTransform headTransform) {
    // Build the camera matrix and apply it to the ModelView.
    Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f);
    //float []ypr = new float[3];
    //headTransform.getEulerAngles(ypr, 0);
    //headTransform.getHeadView(headView, 0);
    //float []headforward = new float[3];
    //headTransform.getForwardVector(headforward, 0);

    Quaternion headRotation = new Quaternion();
    float[]quat = new float[4];
    headTransform.getQuaternion(quat, 0);
    headRotation.set(quat[0], quat[1], quat[2], quat[3]);


    /*
    Quaternion q = new Quaternion();
    float[]quat = new float[4];
    headTransform.getQuaternion(quat, 0);
    q.set(quat[0], quat[1], quat[2], quat[3]);
    Vector3d headforwardVector = q.transform(new Vector3d(1,0,0));
    float []headforward = new float[3];
    headforward[0] = (float)headforwardVector.x;
    headforward[1] = (float)headforwardVector.y;
    headforward[2] = (float)headforwardVector.z;
    */


    //Matrix3x3d.mult(headTransform.);
    synchronized (synHeadPositionClick) {
      if (baseQuaternion == null)
      {
        baseQuaternion = headRotation;
      }
      /*if (baseHeadPosition == null) {
        MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Stop, this);
        prevCommand = MotorWiFiControl.Command.Stop;
        baseHeadPosition = headforwardVector;
      }*/

      /*
      Vector3d headXZ = new Vector3d(headforward[0], 0, headforward[2]);
      headXZ.normalize();
      Vector3d baseForwardXZ = new Vector3d(baseHeadPosition.x, 0, baseHeadPosition.z);
      baseForwardXZ.normalize();
      //double rotY = Math.toDegrees(Math.acos(Vector3d.dot(headXZ, baseForwardXZ)));
      double rotY = Math.toDegrees(calculateAngle(headXZ, baseForwardXZ, Vector3dY));

      Vector3d headYZ = new Vector3d(0, headforward[1], headforward[2]);
      headYZ.normalize();
      Vector3d baseForwardYZ = new Vector3d(0, baseHeadPosition.y, baseHeadPosition.z);
      baseForwardYZ.normalize();
      //double rotX = Math.toDegrees(Math.acos(Vector3d.dot(headYZ, baseForwardYZ)));
      double rotX = Math.toDegrees(calculateAngle(headYZ, baseForwardYZ, Vector3dX));
      */

      Vector3d hfold = baseQuaternion.transform(new Vector3d(1,0,0));
      Vector3d hfnew = headRotation.transform(new Vector3d(0,0,1));
      double rotY = Math.toDegrees(calculateAngle(hfold, hfnew));

      Vector3d huold = baseQuaternion.transform(new Vector3d(0,0,1));
      Vector3d hunew = headRotation.transform(new Vector3d(0,1,0));
      double rotX = Math.toDegrees(calculateAngle(huold, hunew));


      if (rotY < 90 - AppSettings.TurningAngle)
      {
        //if (prevCommand != MotorWiFiControl.Command.Left) {
          //MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Left, this);
        if (mTcpClient != null) {
          mTcpClient.sendMessage("lt");
        }
        prevCommand = MotorWiFiControl.Command.Left;
        //}
      } else if (rotY > 90 + AppSettings.TurningAngle)
      {
        //if (prevCommand != MotorWiFiControl.Command.Right) {
          //MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Right, this);
        if (mTcpClient != null) {
          mTcpClient.sendMessage("rt");
        }
        prevCommand = MotorWiFiControl.Command.Right;
        //}
      }
      else if (rotX < 90 - AppSettings.MotionAngle)
      {
        //if (prevCommand != MotorWiFiControl.Command.Backward) {
        if (mTcpClient != null) {
          mTcpClient.sendMessage("bk");
        }
        //MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Backward, this);
          prevCommand = MotorWiFiControl.Command.Backward;
        //}
      }
      else if (rotX > 90 + AppSettings.MotionAngle)
      {
        //if (prevCommand != MotorWiFiControl.Command.Forward) {
          //MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Forward, this);
        //sends the message to the server
        if (mTcpClient != null) {
          mTcpClient.sendMessage("fw");
        }

        prevCommand = MotorWiFiControl.Command.Forward;
        //}
      }
      else
      {
        //if (prevCommand != MotorWiFiControl.Command.Stop) {
          //MotorWiFiControl.SendRequest(AppSettings.RobotIp, MotorWiFiControl.Command.Stop, this);
        if (mTcpClient != null) {
          mTcpClient.sendMessage("st");
        }
        prevCommand = MotorWiFiControl.Command.Stop;
        //}
      }
    }

    /*
    // Update the 3d audio engine with the most recent head rotation.
    headTransform.getQuaternion(headRotation, 0);
    gvrAudioEngine.setHeadRotation(
        headRotation[0], headRotation[1], headRotation[2], headRotation[3]);
    // Regular update call to GVR audio engine.
    gvrAudioEngine.update();

    Util.checkGlError("onNewFrame");*/
  }

  protected Object bmpSyn = new Object();
  Square square = null;
  Bitmap textureBitmap = null;
  Random r = new Random();
  /**
   * Draws a frame for an eye.
   *
   * @param eye The eye to render. Includes all required transformations.
   */
  @Override
  public void onDrawEye(Eye eye) {
    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    GLES20.glClearColor(1,0,0,1);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    float []mm = new float[16];
    Matrix.setIdentityM(mm, 0);
    Matrix.multiplyMM(view, 0, mm, 0, camera, 0);

    float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

    Matrix.multiplyMM(modelView, 0, view, 0, modelTarget, 0);
    Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);

    GLES20.glUseProgram(objectProgram);
    GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0);

    Bitmap helper = null;
    synchronized (view1.syn)
    {
      //if (thread != null)
      {
        if (view1.lastBitmap != null)
        {
          helper = Bitmap.createBitmap(view1.lastBitmap);
          helper = helper.copy( helper.getConfig() , true);
          //textureBitmap = Bitmap.createScaledBitmap(view1.lastBitmap, textureBitmap.getWidth() * 2, textureBitmap.getHeight(), false);
        }
      }
    }

    if (eye.getType() == Eye.Type.RIGHT) {
      Color cc = Color.valueOf((float)r.nextFloat(),(float)r.nextFloat(),(float)r.nextFloat(),(float)r.nextFloat());
      for (int a = 0; a < 100; a++)
        for (int b = 0; b < 100; b++)
          textureBitmap.setPixel(a, b, cc.toArgb());
      //rightTex.update(textureBitmap);
      if (helper != null) {
        Bitmap bb = Bitmap.createBitmap(helper,
                (int) (helper.getWidth() / 2.0),
                (int) (0),
                (int) (helper.getWidth() / 2.0),
                (int) (helper.getHeight()));

        rightTex.update(Bitmap.createScaledBitmap(bb, textureBitmap.getWidth(), textureBitmap.getHeight(),
                false));

      }
      rightTex.bind();
      rightEye.draw();
    } else if (eye.getType() == Eye.Type.LEFT) {
      if (helper != null) {
        Bitmap bb = Bitmap.createBitmap(helper,
                (int) (0),
                (int) (0),
                (int) (helper.getWidth() / 2.0),
                (int) (helper.getHeight()));

        leftTex.update(Bitmap.createScaledBitmap(bb, textureBitmap.getWidth(), textureBitmap.getHeight(),
                false));
      }
      leftTex.bind();
      leftEye.draw();
    }
    Util.checkGlError("drawTarget");
    /*else if (eye.getType() == Eye.Type.LEFT) {
      GLES20.glEnable(GLES20.GL_DEPTH_TEST);
      // The clear color doesn't matter here because it's completely obscured by
      // the room. However, the color buffer is still cleared because it may
      // improve performance.
      GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

      // Apply the eye transformation to the camera.
      Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

      // Build the ModelView and ModelViewProjection matrices
      // for calculating the position of the target object.
      float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);

      Matrix.multiplyMM(modelView, 0, view, 0, modelTarget, 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
      drawTarget();

      // Set modelView for the room, so it's drawn in the correct location
      Matrix.multiplyMM(modelView, 0, view, 0, modelRoom, 0);
      Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
      drawRoom();
    }*/
  }

  @Override
  public void onFinishFrame(Viewport viewport) {}

  /** Draw the target object. */
  public void drawTarget() {
    GLES20.glUseProgram(objectProgram);
    GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0);
    //if (isLookingAtTarget()) {
    //  targetObjectSelectedTextures.get(curTargetObject).bind();
    //} else {
      //targetObjectNotSelectedTextures.get(curTargetObject).bind();
    //}
    targetObjectMeshes.get(curTargetObject).draw();
    Util.checkGlError("drawTarget");
  }

  /** Draw the room. */
  public void drawRoom() {
    GLES20.glUseProgram(objectProgram);
    GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0);
    roomTex.bind();
    room.draw();
    Util.checkGlError("drawRoom");
  }

  /**
   * Called when the Cardboard trigger is pulled.
   */
  @Override
  public void onCardboardTrigger() {
    Log.i(TAG, "onCardboardTrigger");
    synchronized (synHeadPositionClick) {
        //baseHeadPosition = null;
        baseQuaternion = null;
    }
    if (isLookingAtTarget()) {
      successSourceId = gvrAudioEngine.createStereoSound(SUCCESS_SOUND_FILE);
      gvrAudioEngine.playSound(successSourceId, false /* looping disabled */);
      hideTarget();
    }
  }

  /** Find a new random position for the target object. */
  private void hideTarget() {
    float[] rotationMatrix = new float[16];
    float[] posVec = new float[4];

    // Matrix.setRotateM takes the angle in degrees, but Math.tan takes the angle in radians, so
    // yaw is in degrees and pitch is in radians.
    float yawDegrees = (random.nextFloat() - 0.5f) * 2.0f * MAX_YAW;
    float pitchRadians = (float) Math.toRadians((random.nextFloat() - 0.5f) * 2.0f * MAX_PITCH);

    Matrix.setRotateM(rotationMatrix, 0, yawDegrees, 0.0f, 1.0f, 0.0f);
    targetDistance =
        random.nextFloat() * (MAX_TARGET_DISTANCE - MIN_TARGET_DISTANCE) + MIN_TARGET_DISTANCE;
    targetPosition = new float[] {0.0f, 0.0f, -targetDistance};
    Matrix.setIdentityM(modelTarget, 0);
    Matrix.translateM(modelTarget, 0, targetPosition[0], targetPosition[1], targetPosition[2]);
    Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, modelTarget, 12);

    targetPosition[0] = posVec[0];
    targetPosition[1] = (float) Math.tan(pitchRadians) * targetDistance;
    targetPosition[2] = posVec[2];

    updateTargetPosition();
    curTargetObject = random.nextInt(TARGET_MESH_COUNT);
  }

  /**
   * Check if user is looking at the target object by calculating where the object is in eye-space.
   *
   * @return true if the user is looking at the target object.
   */
  private boolean isLookingAtTarget() {
    // Convert object space to camera space. Use the headView from onNewFrame.
    Matrix.multiplyMM(modelView, 0, headView, 0, modelTarget, 0);
    Matrix.multiplyMV(tempPosition, 0, modelView, 0, POS_MATRIX_MULTIPLY_VEC, 0);

    float angle = Util.angleBetweenVectors(tempPosition, FORWARD_VEC);
    return angle < ANGLE_LIMIT;
  }

  boolean mRun = false;
  //tututututu
  public class MjpegViewThread extends Thread {
    private int frameCounter = 0;
    private long start;
    private Bitmap ovl;

    public MjpegViewThread() {
    }

    public Bitmap bm = null;

    public void run() {
      start = System.currentTimeMillis();
      PorterDuffXfermode mode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

      int width;
      int height;
      Rect destRect;
      Canvas c = null;
      Paint p = new Paint();
      String fps;
      while (mRun) {
          try {
            //c = mSurfaceHolder.lockCanvas();
            //synchronized (bmpSyn)
            {
              try {
                bm = mIn.readMjpegFrame();
              } catch (IOException e) {
                e.getStackTrace();
                Log.d(TAG, "catch IOException hit in run", e);
              }
              /*
              try {
                Thread.sleep(1);
              } catch (InterruptedException e) {
                e.printStackTrace();
              }*/
            }
          } finally {
            if (c != null) {
              //mSurfaceHolder.unlockCanvasAndPost(c);
            }
        }
      }
    }

  }
  private MjpegViewThread thread;
  private MjpegInputStream mIn = null;
  public void startPlayback() {
    if(mIn != null) {
      mRun = true;
      thread.start();
    }
  }

  public void stopPlayback() {
    mRun = false;
    boolean retry = true;
    while(retry) {
      try {
        thread.join();
        retry = false;
      } catch (InterruptedException e) {
        e.getStackTrace();
        Log.d(TAG, "catch IOException hit in stopPlayback", e);
      }
    }
  }

  public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
    protected MjpegInputStream doInBackground(String... url) {
      //TODO: if camera has authentication deal with it and don't just not work
      HttpResponse res = null;
      DefaultHttpClient httpclient = new DefaultHttpClient();
      Log.d(TAG, "1. Sending http request");
      try {
        res = httpclient.execute(new HttpGet(URI.create(url[0])));
        Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
        if(res.getStatusLine().getStatusCode()==401){
          //You must turn off camera User Access Control before this will work
          return null;
        }
        return new MjpegInputStream(res.getEntity().getContent());
      } catch (ClientProtocolException e) {
        e.printStackTrace();
        Log.d(TAG, "Request failed-ClientProtocolException", e);
        //Error connecting to camera
      } catch (IOException e) {
        e.printStackTrace();
        Log.d(TAG, "Request failed-IOException", e);
        //Error connecting to camera
      }

      return null;
    }

    protected void onPostExecute(MjpegInputStream result) {
      setSource(result);
    }
  }
  public void setSource(MjpegInputStream source) {
    mIn = source;
    startPlayback();
  }
}
