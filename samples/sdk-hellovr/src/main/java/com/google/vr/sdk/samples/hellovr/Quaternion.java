package com.google.vr.sdk.samples.hellovr;

/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
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
 ******************************************************************************/

import com.google.vr.sdk.base.sensors.internal.Vector3d;

import java.io.Serializable;


/** A simple quaternion class.
 * @see <a href="http://en.wikipedia.org/wiki/Quaternion">http://en.wikipedia.org/wiki/Quaternion</a>
 * @author badlogicgames@gmail.com
 * @author vesuvio
 * @author xoppa */
public class Quaternion implements Serializable {
    private static final long serialVersionUID = -7661875440774897168L;
    private static Quaternion tmp1 = new Quaternion(0, 0, 0, 0);
    private static Quaternion tmp2 = new Quaternion(0, 0, 0, 0);

    public double x;
    public double y;
    public double z;
    public double w;

    /** Constructor, sets the four components of the quaternion.
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component */
    public Quaternion (double x, double y, double z, double w) {
        this.set(x, y, z, w);
    }

    public Quaternion () {
        idt();
    }

    /** Constructor, sets the quaternion components from the given quaternion.
     *
     * @param quaternion The quaternion to copy. */
    public Quaternion (Quaternion quaternion) {
        this.set(quaternion);
    }

    /** Constructor, sets the quaternion from the given axis vector and the angle around that axis in degrees.
     *
     * @param axis The axis
     * @param angle The angle in degrees. */
    /*public Quaternion (Vector3d axis, double angle) {
        this.set(axis, angle);
    }*/

    /** Sets the components of the quaternion
     * @param x The x-component
     * @param y The y-component
     * @param z The z-component
     * @param w The w-component
     * @return This quaternion for chaining */
    public Quaternion set (double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /** Sets the quaternion components from the given quaternion.
     * @param quaternion The quaternion.
     * @return This quaternion for chaining. */
    public Quaternion set (Quaternion quaternion) {
        return this.set(quaternion.x, quaternion.y, quaternion.z, quaternion.w);
    }

    /** Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis The axis
     * @param angle The angle in degrees
     * @return This quaternion for chaining. */
    /*public Quaternion set (Vector3d axis, double angle) {
        return setFromAxis(axis.x, axis.y, axis.z, angle);
    }*/

    /** @return a copy of this quaternion */
    public Quaternion cpy () {
        return new Quaternion(this);
    }

    /** @return the euclidean length of the specified quaternion */
    public final static double len (final double x, final double y, final double z, final double w) {
        return (double)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    /** @return the euclidean length of this quaternion */
    public double len () {
        return (double)Math.sqrt(x * x + y * y + z * z + w * w);
    }

    @Override
    public String toString () {
        return "[" + x + "|" + y + "|" + z + "|" + w + "]";
    }

    /** Sets the quaternion to the given euler angles in degrees.
     * @param yaw the rotation around the y axis in degrees
     * @param pitch the rotation around the x axis in degrees
     * @param roll the rotation around the z axis degrees
     * @return this quaternion */
    public Quaternion setEulerAngles (double yaw, double pitch, double roll) {
        return setEulerAnglesRad(yaw * MathUtils.degreesToRadians, pitch * MathUtils.degreesToRadians, roll
                * MathUtils.degreesToRadians);
    }

    /** Sets the quaternion to the given euler angles in radians.
     * @param yaw the rotation around the y axis in radians
     * @param pitch the rotation around the x axis in radians
     * @param roll the rotation around the z axis in radians
     * @return this quaternion */
    public Quaternion setEulerAnglesRad (double yaw, double pitch, double roll) {
        final double hr = roll * 0.5f;
        final double shr = (double)Math.sin(hr);
        final double chr = (double)Math.cos(hr);
        final double hp = pitch * 0.5f;
        final double shp = (double)Math.sin(hp);
        final double chp = (double)Math.cos(hp);
        final double hy = yaw * 0.5f;
        final double shy = (double)Math.sin(hy);
        final double chy = (double)Math.cos(hy);
        final double chy_shp = chy * shp;
        final double shy_chp = shy * chp;
        final double chy_chp = chy * chp;
        final double shy_shp = shy * shp;

        x = (chy_shp * chr) + (shy_chp * shr); // cos(yaw/2) * sin(pitch/2) * cos(roll/2) + sin(yaw/2) * cos(pitch/2) * sin(roll/2)
        y = (shy_chp * chr) - (chy_shp * shr); // sin(yaw/2) * cos(pitch/2) * cos(roll/2) - cos(yaw/2) * sin(pitch/2) * sin(roll/2)
        z = (chy_chp * shr) - (shy_shp * chr); // cos(yaw/2) * cos(pitch/2) * sin(roll/2) - sin(yaw/2) * sin(pitch/2) * cos(roll/2)
        w = (chy_chp * chr) + (shy_shp * shr); // cos(yaw/2) * cos(pitch/2) * cos(roll/2) + sin(yaw/2) * sin(pitch/2) * sin(roll/2)
        return this;
    }

    /** Get the pole of the gimbal lock, if any.
     * @return positive (+1) for north pole, negative (-1) for south pole, zero (0) when no gimbal lock */
    public int getGimbalPole () {
        final double t = y * x + z * w;
        return t > 0.499f ? 1 : (t < -0.499f ? -1 : 0);
    }

    /** Get the roll euler angle in radians, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * @return the rotation around the z axis in radians (between -PI and +PI) */
    public double getRollRad () {
        final int pole = getGimbalPole();
        return pole == 0 ? MathUtils.atan2(2f * (w * z + y * x), 1f - 2f * (x * x + z * z)) : (double)pole * 2f
                * MathUtils.atan2(y, w);
    }

    /** Get the roll euler angle in degrees, which is the rotation around the z axis. Requires that this quaternion is normalized.
     * @return the rotation around the z axis in degrees (between -180 and +180) */
    public double getRoll () {
        return getRollRad() * MathUtils.radiansToDegrees;
    }

    /** Get the pitch euler angle in radians, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * @return the rotation around the x axis in radians (between -(PI/2) and +(PI/2)) */
    public double getPitchRad () {
        final int pole = getGimbalPole();
        return pole == 0 ? (double)Math.asin(MathUtils.clamp(2f * (w * x - z * y), -1f, 1f)) : (double)pole * MathUtils.PI * 0.5f;
    }

    /** Get the pitch euler angle in degrees, which is the rotation around the x axis. Requires that this quaternion is normalized.
     * @return the rotation around the x axis in degrees (between -90 and +90) */
    public double getPitch () {
        return getPitchRad() * MathUtils.radiansToDegrees;
    }

    /** Get the yaw euler angle in radians, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * @return the rotation around the y axis in radians (between -PI and +PI) */
    public double getYawRad () {
        return getGimbalPole() == 0 ? MathUtils.atan2(2f * (y * w + x * z), 1f - 2f * (y * y + x * x)) : 0f;
    }

    /** Get the yaw euler angle in degrees, which is the rotation around the y axis. Requires that this quaternion is normalized.
     * @return the rotation around the y axis in degrees (between -180 and +180) */
    public double getYaw () {
        return getYawRad() * MathUtils.radiansToDegrees;
    }

    public final static double len2 (final double x, final double y, final double z, final double w) {
        return x * x + y * y + z * z + w * w;
    }

    /** @return the length of this quaternion without square root */
    public double len2 () {
        return x * x + y * y + z * z + w * w;
    }

    /** Normalizes this quaternion to unit length
     * @return the quaternion for chaining */
    public Quaternion nor () {
        double len = len2();
        if (len != 0.f && !MathUtils.isEqual(len, 1f)) {
            len = (double)Math.sqrt(len);
            w /= len;
            x /= len;
            y /= len;
            z /= len;
        }
        return this;
    }

    /** Conjugate the quaternion.
     *
     * @return This quaternion for chaining */
    public Quaternion conjugate () {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    // TODO : this would better fit into the Vector3d class
    /** Transforms the given vector using this quaternion
     *
     * @param v Vector to transform */
    public Vector3d transform (Vector3d v) {
        tmp2.set(this);
        tmp2.conjugate();
        tmp2.mulLeft(tmp1.set(v.x, v.y, v.z, 0)).mulLeft(this);

        v.x = tmp2.x;
        v.y = tmp2.y;
        v.z = tmp2.z;
        return v;
    }

    /** Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining */
    public Quaternion mul (final Quaternion other) {
        final double newX = this.w * other.x + this.x * other.w + this.y * other.z - this.z * other.y;
        final double newY = this.w * other.y + this.y * other.w + this.z * other.x - this.x * other.z;
        final double newZ = this.w * other.z + this.z * other.w + this.x * other.y - this.y * other.x;
        final double newW = this.w * other.w - this.x * other.x - this.y * other.y - this.z * other.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /** Multiplies this quaternion with another one in the form of this = this * other
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining */
    public Quaternion mul (final double x, final double y, final double z, final double w) {
        final double newX = this.w * x + this.x * w + this.y * z - this.z * y;
        final double newY = this.w * y + this.y * w + this.z * x - this.x * z;
        final double newZ = this.w * z + this.z * w + this.x * y - this.y * x;
        final double newW = this.w * w - this.x * x - this.y * y - this.z * z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /** Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param other Quaternion to multiply with
     * @return This quaternion for chaining */
    public Quaternion mulLeft (Quaternion other) {
        final double newX = other.w * this.x + other.x * this.w + other.y * this.z - other.z * this.y;
        final double newY = other.w * this.y + other.y * this.w + other.z * this.x - other.x * this.z;
        final double newZ = other.w * this.z + other.z * this.w + other.x * this.y - other.y * this.x;
        final double newW = other.w * this.w - other.x * this.x - other.y * this.y - other.z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /** Multiplies this quaternion with another one in the form of this = other * this
     *
     * @param x the x component of the other quaternion to multiply with
     * @param y the y component of the other quaternion to multiply with
     * @param z the z component of the other quaternion to multiply with
     * @param w the w component of the other quaternion to multiply with
     * @return This quaternion for chaining */
    public Quaternion mulLeft (final double x, final double y, final double z, final double w) {
        final double newX = w * this.x + x * this.w + y * this.z - z * this.y;
        final double newY = w * this.y + y * this.w + z * this.x - x * this.z;
        final double newZ = w * this.z + z * this.w + x * this.y - y * this.x;
        final double newW = w * this.w - x * this.x - y * this.y - z * this.z;
        this.x = newX;
        this.y = newY;
        this.z = newZ;
        this.w = newW;
        return this;
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion */
    public Quaternion add (Quaternion quaternion) {
        this.x += quaternion.x;
        this.y += quaternion.y;
        this.z += quaternion.z;
        this.w += quaternion.w;
        return this;
    }

    /** Add the x,y,z,w components of the passed in quaternion to the ones of this quaternion */
    public Quaternion add (double qx, double qy, double qz, double qw) {
        this.x += qx;
        this.y += qy;
        this.z += qz;
        this.w += qw;
        return this;
    }


    /** Sets the quaternion to an identity Quaternion
     * @return this quaternion for chaining */
    public Quaternion idt () {
        return this.set(0, 0, 0, 1);
    }

    /** @return If this quaternion is an identity Quaternion */
    public boolean isIdentity () {
        return MathUtils.isZero(x) && MathUtils.isZero(y) && MathUtils.isZero(z) && MathUtils.isEqual(w, 1f);
    }

    /** @return If this quaternion is an identity Quaternion */
    public boolean isIdentity (final double tolerance) {
        return MathUtils.isZero(x, tolerance) && MathUtils.isZero(y, tolerance) && MathUtils.isZero(z, tolerance)
                && MathUtils.isEqual(w, 1f, tolerance);
    }

    // todo : the setFromAxis(v3,double) method should replace the set(v3,double) method
    /** Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis The axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining. */
    /*public Quaternion setFromAxis (final Vector3d axis, final double degrees) {
        return setFromAxis(axis.x, axis.y, axis.z, degrees);
    }*/

    /** Sets the quaternion components from the given axis and angle around that axis.
     *
     * @param axis The axis
     * @param radians The angle in radians
     * @return This quaternion for chaining. */
  /*  public Quaternion setFromAxisRad (final Vector3d axis, final double radians) {
        return setFromAxisRad(axis.x, axis.y, axis.z, radians);
    }
*/
    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param degrees The angle in degrees
     * @return This quaternion for chaining. */
  /*  public Quaternion setFromAxis (final double x, final double y, final double z, final double degrees) {
        return setFromAxisRad(x, y, z, degrees * MathUtils.degreesToRadians);
    }
*/
    /** Sets the quaternion components from the given axis and angle around that axis.
     * @param x X direction of the axis
     * @param y Y direction of the axis
     * @param z Z direction of the axis
     * @param radians The angle in radians
     * @return This quaternion for chaining. */
    /*public Quaternion setFromAxisRad (final double x, final double y, final double z, final double radians) {
        double d = Vector3d.len(x, y, z);
        if (d == 0f) return idt();
        d = 1f / d;
        double l_ang = radians < 0 ? MathUtils.PI2 - (-radians % MathUtils.PI2) : radians % MathUtils.PI2;
        double l_sin = (double)Math.sin(l_ang / 2);
        double l_cos = (double)Math.cos(l_ang / 2);
        return this.set(d * x * l_sin, d * y * l_sin, d * z * l_sin, l_cos).nor();
    }*/

    /** Sets the Quaternion from the given matrix, optionally removing any scaling. */
  /*  public Quaternion setFromMatrix (boolean normalizeAxes, Matrix4 matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Matrix4.M00], matrix.val[Matrix4.M01], matrix.val[Matrix4.M02],
                matrix.val[Matrix4.M10], matrix.val[Matrix4.M11], matrix.val[Matrix4.M12], matrix.val[Matrix4.M20],
                matrix.val[Matrix4.M21], matrix.val[Matrix4.M22]);
    }
*/

    /** Sets the Quaternion from the given matrix, optionally removing any scaling. */
  /*  public Quaternion setFromMatrix (boolean normalizeAxes, Matrix3 matrix) {
        return setFromAxes(normalizeAxes, matrix.val[Matrix3.M00], matrix.val[Matrix3.M01], matrix.val[Matrix3.M02],
                matrix.val[Matrix3.M10], matrix.val[Matrix3.M11], matrix.val[Matrix3.M12], matrix.val[Matrix3.M20],
                matrix.val[Matrix3.M21], matrix.val[Matrix3.M22]);
    }
*/
    /** Sets the Quaternion from the given rotation matrix, which must not contain scaling. */
  /*  public Quaternion setFromMatrix (Matrix3 matrix) {
        return setFromMatrix(false, matrix);
    }
*/
    /** <p>
     * Sets the Quaternion from the given x-, y- and z-axis which have to be orthonormal.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate */
  /*  public Quaternion setFromAxes (double xx, double xy, double xz, double yx, double yy, double yz, double zx, double zy, double zz) {
        return setFromAxes(false, xx, xy, xz, yx, yy, yz, zx, zy, zz);
    }
*/
    /** <p>
     * Sets the Quaternion from the given x-, y- and z-axis.
     * </p>
     *
     * <p>
     * Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/ which in turn took it from Graphics Gem code at
     * ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z.
     * </p>
     *
     * @param normalizeAxes whether to normalize the axes (necessary when they contain scaling)
     * @param xx x-axis x-coordinate
     * @param xy x-axis y-coordinate
     * @param xz x-axis z-coordinate
     * @param yx y-axis x-coordinate
     * @param yy y-axis y-coordinate
     * @param yz y-axis z-coordinate
     * @param zx z-axis x-coordinate
     * @param zy z-axis y-coordinate
     * @param zz z-axis z-coordinate */
  /*  public Quaternion setFromAxes (boolean normalizeAxes, double xx, double xy, double xz, double yx, double yy, double yz, double zx,
                                   double zy, double zz) {
        if (normalizeAxes) {
            final double lx = 1f / Vector3d.len(xx, xy, xz);
            final double ly = 1f / Vector3d.len(yx, yy, yz);
            final double lz = 1f / Vector3d.len(zx, zy, zz);
            xx *= lx;
            xy *= lx;
            xz *= lx;
            yx *= ly;
            yy *= ly;
            yz *= ly;
            zx *= lz;
            zy *= lz;
            zz *= lz;
        }
        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final double t = xx + yy + zz;

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            double s = (double)Math.sqrt(t + 1); // |s|>=1 ...
            w = 0.5f * s;
            s = 0.5f / s; // so this division isn't bad
            x = (zy - yz) * s;
            y = (xz - zx) * s;
            z = (yx - xy) * s;
        } else if ((xx > yy) && (xx > zz)) {
            double s = (double)Math.sqrt(1.0 + xx - yy - zz); // |s|>=1
            x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            y = (yx + xy) * s;
            z = (xz + zx) * s;
            w = (zy - yz) * s;
        } else if (yy > zz) {
            double s = (double)Math.sqrt(1.0 + yy - xx - zz); // |s|>=1
            y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            x = (yx + xy) * s;
            z = (zy + yz) * s;
            w = (xz - zx) * s;
        } else {
            double s = (double)Math.sqrt(1.0 + zz - xx - yy); // |s|>=1
            z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            x = (xz + zx) * s;
            y = (zy + yz) * s;
            w = (yx - xy) * s;
        }

        return this;
    }
*/
    /** Set this quaternion to the rotation between two vectors.
     * @param v1 The base vector, which should be normalized.
     * @param v2 The target vector, which should be normalized.
     * @return This quaternion for chaining */
  /*  public Quaternion setFromCross (final Vector3d v1, final Vector3d v2) {
        final double dot = MathUtils.clamp(v1.dot(v2), -1f, 1f);
        final double angle = (double)Math.acos(dot);
        return setFromAxisRad(v1.y * v2.z - v1.z * v2.y, v1.z * v2.x - v1.x * v2.z, v1.x * v2.y - v1.y * v2.x, angle);
    }
*/
    /** Set this quaternion to the rotation between two vectors.
     * @param x1 The base vectors x value, which should be normalized.
     * @param y1 The base vectors y value, which should be normalized.
     * @param z1 The base vectors z value, which should be normalized.
     * @param x2 The target vector x value, which should be normalized.
     * @param y2 The target vector y value, which should be normalized.
     * @param z2 The target vector z value, which should be normalized.
     * @return This quaternion for chaining */
  /*  public Quaternion setFromCross (final double x1, final double y1, final double z1, final double x2, final double y2, final double z2) {
        final double dot = MathUtils.clamp(Vector3d.dot(x1, y1, z1, x2, y2, z2), -1f, 1f);
        final double angle = (double)Math.acos(dot);
        return setFromAxisRad(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2, x1 * y2 - y1 * x2, angle);
    }
*/
    /** Spherical linear interpolation between this quaternion and the other quaternion, based on the alpha value in the range
     * [0,1]. Taken from Bones framework for JPCT, see http://www.aptalkarga.com/bones/
     * @param end the end quaternion
     * @param alpha alpha in the range [0,1]
     * @return this quaternion for chaining */
    public Quaternion slerp (Quaternion end, double alpha) {
        final double d = this.x * end.x + this.y * end.y + this.z * end.z + this.w * end.w;
        double absDot = d < 0.f ? -d : d;

        // Set the first and second scale for the interpolation
        double scale0 = 1f - alpha;
        double scale1 = alpha;

        // Check if the angle between the 2 quaternions was big enough to
        // warrant such calculations
        if ((1 - absDot) > 0.1) {// Get the angle between the 2 quaternions,
            // and then store the sin() of that angle
            final double angle = (double)Math.acos(absDot);
            final double invSinTheta = 1f / (double)Math.sin(angle);

            // Calculate the scale for q1 and q2, according to the angle and
            // it's sine value
            scale0 = ((double)Math.sin((1f - alpha) * angle) * invSinTheta);
            scale1 = ((double)Math.sin((alpha * angle)) * invSinTheta);
        }

        if (d < 0.f) scale1 = -scale1;

        // Calculate the x, y, z and w values for the quaternion by using a
        // special form of linear interpolation for quaternions.
        x = (scale0 * x) + (scale1 * end.x);
        y = (scale0 * y) + (scale1 * end.y);
        z = (scale0 * z) + (scale1 * end.z);
        w = (scale0 * w) + (scale1 * end.w);

        // Return the interpolated quaternion
        return this;
    }

    /** Spherical linearly interpolates multiple quaternions and stores the result in this Quaternion. Will not destroy the data
     * previously inside the elements of q. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where w_i=1/n.
     * @param q List of quaternions
     * @return This quaternion for chaining */
    public Quaternion slerp (Quaternion[] q) {

        // Calculate exponents and multiply everything from left to right
        final double w = 1.0f / q.length;
        set(q[0]).exp(w);
        for (int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w));
        nor();
        return this;
    }

    /** Spherical linearly interpolates multiple quaternions by the given weights and stores the result in this Quaternion. Will not
     * destroy the data previously inside the elements of q or w. result = (q_1^w_1)*(q_2^w_2)* ... *(q_n^w_n) where the sum of w_i
     * is 1. Lists must be equal in length.
     * @param q List of quaternions
     * @param w List of weights
     * @return This quaternion for chaining */
    public Quaternion slerp (Quaternion[] q, double[] w) {

        // Calculate exponents and multiply everything from left to right
        set(q[0]).exp(w[0]);
        for (int i = 1; i < q.length; i++)
            mul(tmp1.set(q[i]).exp(w[i]));
        nor();
        return this;
    }

    /** Calculates (this quaternion)^alpha where alpha is a real number and stores the result in this quaternion. See
     * http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
     * @param alpha Exponent
     * @return This quaternion for chaining */
    public Quaternion exp (double alpha) {

        // Calculate |q|^alpha
        double norm = len();
        double normExp = (double)Math.pow(norm, alpha);

        // Calculate theta
        double theta = (double)Math.acos(w / norm);

        // Calculate coefficient of basis elements
        double coeff = 0;
        if (Math.abs(theta) < 0.001) // If theta is small enough, use the limit of sin(alpha*theta) / sin(theta) instead of actual
// value
            coeff = normExp * alpha / norm;
        else
            coeff = (double)(normExp * Math.sin(alpha * theta) / (norm * Math.sin(theta)));

        // Write results
        w = (double)(normExp * Math.cos(alpha * theta));
        x *= coeff;
        y *= coeff;
        z *= coeff;

        // Fix any possible discrepancies
        nor();

        return this;
    }

    //@Override
  /*  public int hashCode () {
        final int prime = 31;
        int result = 1;
        result = prime * result + NumberUtils.doubleToRawIntBits(w);
        result = prime * result + NumberUtils.doubleToRawIntBits(x);
        result = prime * result + NumberUtils.doubleToRawIntBits(y);
        result = prime * result + NumberUtils.doubleToRawIntBits(z);
        return result;
    }
*/
    //@Override
    /*
    public boolean equals (Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Quaternion)) {
            return false;
        }
        Quaternion other = (Quaternion)obj;
        return (NumberUtils.doubleToRawIntBits(w) == NumberUtils.doubleToRawIntBits(other.w))
                && (NumberUtils.doubleToRawIntBits(x) == NumberUtils.doubleToRawIntBits(other.x))
                && (NumberUtils.doubleToRawIntBits(y) == NumberUtils.doubleToRawIntBits(other.y))
                && (NumberUtils.doubleToRawIntBits(z) == NumberUtils.doubleToRawIntBits(other.z));
    }
*/
    /** Get the dot product between the two quaternions (commutative).
     * @param x1 the x component of the first quaternion
     * @param y1 the y component of the first quaternion
     * @param z1 the z component of the first quaternion
     * @param w1 the w component of the first quaternion
     * @param x2 the x component of the second quaternion
     * @param y2 the y component of the second quaternion
     * @param z2 the z component of the second quaternion
     * @param w2 the w component of the second quaternion
     * @return the dot product between the first and second quaternion. */
    public final static double dot (final double x1, final double y1, final double z1, final double w1, final double x2, final double y2,
                                   final double z2, final double w2) {
        return x1 * x2 + y1 * y2 + z1 * z2 + w1 * w2;
    }

    /** Get the dot product between this and the other quaternion (commutative).
     * @param other the other quaternion.
     * @return the dot product of this and the other quaternion. */
    public double dot (final Quaternion other) {
        return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
    }

    /** Get the dot product between this and the other quaternion (commutative).
     * @param x the x component of the other quaternion
     * @param y the y component of the other quaternion
     * @param z the z component of the other quaternion
     * @param w the w component of the other quaternion
     * @return the dot product of this and the other quaternion. */
    public double dot (final double x, final double y, final double z, final double w) {
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    /** Multiplies the components of this quaternion with the given scalar.
     * @param scalar the scalar.
     * @return this quaternion for chaining. */
    public Quaternion mul (double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    /** Get the axis angle representation of the rotation in degrees. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in degrees around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     *
     * @param axis vector which will receive the axis
     * @return the angle in degrees
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a> */
  /*  public double getAxisAngle (Vector3d axis) {
        return getAxisAngleRad(axis) * MathUtils.radiansToDegrees;
    }
*/
    /** Get the axis-angle representation of the rotation in radians. The supplied vector will receive the axis (x, y and z values)
     * of the rotation and the value returned is the angle in radians around that axis. Note that this method will alter the
     * supplied vector, the existing value of the vector is ignored. </p> This will normalize this quaternion if needed. The
     * received axis is a unit vector. However, if this is an identity quaternion (no rotation), then the length of the axis may be
     * zero.
     *
     * @param axis vector which will receive the axis
     * @return the angle in radians
     * @see <a href="http://en.wikipedia.org/wiki/Axis%E2%80%93angle_representation">wikipedia</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToAngle">calculation</a> */
  /*  public double getAxisAngleRad (Vector3d axis) {
        if (this.w > 1) this.nor(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised
        double angle = (double)(2.0 * Math.acos(this.w));
        double s = Math.sqrt(1 - this.w * this.w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s < MathUtils.double_ROUNDING_ERROR) { // test to avoid divide by zero, s is always positive due to sqrt
            // if s close to zero then direction of axis not important
            axis.x = this.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            axis.y = this.y;
            axis.z = this.z;
        } else {
            axis.x = (double)(this.x / s); // normalise axis
            axis.y = (double)(this.y / s);
            axis.z = (double)(this.z / s);
        }

        return angle;
    }
*/


}
