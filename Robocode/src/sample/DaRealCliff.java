/**
 * Copyright (c) 2001-2017 Mathew A. Nelson and Robocode contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://robocode.sourceforge.net/license/epl-v10.html
 */
package sample;


import robocode.HitByBulletEvent;
import robocode.AdvancedRobot;
import robocode.ScannedRobotEvent;
import robocode.util.Utils;
import java.awt.geom.*;

import java.util.ArrayList;
import java.util.List;
import java.awt.*;


/**
 * PaintingRobot - a sample robot that demonstrates the onPaint() and
 * getGraphics() methods.
 * Also demonstrate feature of debugging properties on RobotDialog
 * <p/>
 * Moves in a seesaw motion, and spins the gun around at each end.
 * When painting is enabled for this robot, a red circle will be painted
 * around this robot.
 *
 * @author Stefan Westen (original SGSample)
 * @author Pavel Savara (contributor)
 */


public class DaRealCliff extends AdvancedRobot {

	/**
	 * DaRealCliff's run method
	 */


	//WaveSurfing Variables
	public static int BINS = 47;
	public static double _surfStats[] = new double[BINS];
	public Point2D.Double _myLocation;     // our bot's location
	public Point2D.Double _enemyLocation;  // enemy bot's location

	public ArrayList _enemyWaves;
	public ArrayList _surfDirections;
	public ArrayList _surfAbsBearings;

	public static double _oppEnergy = 100.0;

	/** This is a rectangle that represents an 800x600 battle field,
		 * used for a simple, iterative WallSmoothing method (by PEZ).
		 * The wall stick indicates the amount of space we try to always have on either end of the tank
		 * (extending straight out the front or back) before touching a wall.
	 */
	public static Rectangle2D.Double _fieldRect = new java.awt.geom.Rectangle2D.Double(18, 18, 764, 564);
	public static double WALL_STICK = 160;


	//GuessFactor Targeting Variables
	//List<WaveBullet> waves = new ArrayList<WaveBullet>();
	int[][] stats = new int[13][31];

	int direction = 1;


	public void run() {
		//Wave Surfing
		_enemyWaves = new ArrayList();
		_surfDirections = new ArrayList();
		_surfAbsBearings = new ArrayList();

		//setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);

		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY);
		} while (true);
	}
	double oldEnemyHeading = 0;

	//What the bot does when it sees enemy
	public void onScannedRobot(ScannedRobotEvent e) {
		//WaveSurfing
		_myLocation = new Point2D.Double(getX(), getY());

		double lateralVelocity = getVelocity()*Math.sin(e.getBearingRadians());
		double absBearing = e.getBearingRadians() + getHeadingRadians();

		setTurnRadarRightRadians(Utils.normalRelativeAngle(absBearing
				- getRadarHeadingRadians()) * 2);

		_surfDirections.add(0,
				new Integer((lateralVelocity >= 0) ? 1 : -1));
		_surfAbsBearings.add(0, new Double(absBearing + Math.PI));


		double bulletPower = _oppEnergy - e.getEnergy();
		if (bulletPower < 3.01 && bulletPower > 0.09
				&& _surfDirections.size() > 2) {
			EnemyWave ew = new EnemyWave();
			ew.fireTime = getTime() - 1;
			ew.bulletVelocity = bulletVelocity(bulletPower);
			ew.distanceTraveled = bulletVelocity(bulletPower);
			ew.direction = ((Integer)_surfDirections.get(2)).intValue();
			ew.directAngle = ((Double)_surfAbsBearings.get(2)).doubleValue();
			ew.fireLocation = (Point2D.Double)_enemyLocation.clone(); // last tick

			_enemyWaves.add(ew);
		}

		_oppEnergy = e.getEnergy();

		// update after EnemyWave detection, because that needs the previous
		// enemy location as the source of the wave
		_enemyLocation = project(_myLocation, absBearing, e.getDistance());

		updateWaves();
		doSurfing();
		//WaveSurfing End
		double Power = Math.min(3.0,getEnergy());
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();

		double enemyHeadingChange = enemyHeading - oldEnemyHeading;
		double enemyVelocity = e.getVelocity();
		oldEnemyHeading = enemyHeading;

		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(),
				battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) <
				Point2D.Double.distance(myX, myY, predictedX, predictedY)){
			predictedX += Math.sin(enemyHeading) * enemyVelocity;
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			enemyHeading += enemyHeadingChange;
			if(	predictedX < 18.0
					|| predictedY < 18.0
					|| predictedX > battleFieldWidth - 18.0
					|| predictedY > battleFieldHeight - 18.0){

				predictedX = Math.min(Math.max(18.0, predictedX),
						battleFieldWidth - 18.0);
				predictedY = Math.min(Math.max(18.0, predictedY),
						battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(
				predictedX - getX(), predictedY - getY()));

		setTurnRadarRightRadians(Utils.normalRelativeAngle(
				absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(
				theta - getGunHeadingRadians()));
		fire(3);

/**
		//Guessfactor Targeting
		double eX = getX() + Math.sin(absBearing)*e.getDistance();
		double eY = getY() + Math.cos(absBearing)*e.getDistance();

		//the waves process
		for(int i = 0; i<waves.size(); i++){
			WaveBullet currentWave = (WaveBullet)waves.get(i);
			if (currentWave.checkHit(eX, eY, getTime()))
			{
				waves.remove(currentWave);
				i--;
			}
		}
		//determines the power
		double power = Math.min(3, Math.max(0.1, 1));

		if(e.getVelocity()!=0){
			if(Math.sin(e.getHeadingRadians()-absBearing)*e.getVelocity() <0){
				direction -= 1;
			}else{
				direction = 1;
			}

		}
		int[] currentStats = stats[Math.min(10, (int)((e.getDistance() / (20 - 3 * bulletPower)) / 10))];

		WaveBullet newWave = new WaveBullet(getX(), getY() ,absBearing, power, direction, getTime(), currentStats);

		int bestindex = 15;	// initialize it to be in the middle, guessfactor 0.
		for (int i=0; i<31; i++)
			if (currentStats[bestindex] < currentStats[i])
				bestindex = i;

		// this should do the opposite of the math in the WaveBullet:
		double guessfactor = (double)(bestindex - (stats.length - 1) / 2) / ((stats.length - 1) / 2);
		double angleOffset = direction * guessfactor * newWave.maxEscapeAngle();
		double gunAdjust = Utils.normalRelativeAngle(
				absBearing - getGunHeadingRadians() + angleOffset);
		setTurnGunRightRadians(gunAdjust);
		if (setFireBullet(0) != null)
			waves.add(newWave);
		if (getGunHeat() == 0 && gunAdjust < Math.atan2(9, e.getDistance()) && setFireBullet(0) != null) {

			fire(power);

		}
 **/

	}

	/**
	 * We were hit!  Turn perpendicular to the bullet,
	 * so our seesaw might avoid a future shot.
	 * In addition, draw orange circles where we were hit.
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// demonstrate feature of debugging properties on RobotDialog
		setDebugProperty("lastHitBy", e.getName() + " with power of bullet " + e.getPower() + " at time " + getTime());

		// show how to remove debugging property
		setDebugProperty("lastScannedRobot", null);

		// gebugging by painting to battle view
		Graphics2D g = getGraphics();

		g.setColor(Color.orange);
		g.drawOval((int) (getX() - 55), (int) (getY() - 55), 110, 110);
		g.drawOval((int) (getX() - 56), (int) (getY() - 56), 112, 112);
		g.drawOval((int) (getX() - 59), (int) (getY() - 59), 118, 118);
		g.drawOval((int) (getX() - 60), (int) (getY() - 60), 120, 120);

		//Wave Surfing - when hit by bullet
		if (!_enemyWaves.isEmpty()) {
			Point2D.Double hitBulletLocation = new Point2D.Double(
					e.getBullet().getX(), e.getBullet().getY());
			EnemyWave hitWave = null;

			// look through the EnemyWaves, and find one that could've hit us.
			for (int x = 0; x < _enemyWaves.size(); x++) {
				EnemyWave ew = (EnemyWave)_enemyWaves.get(x);

				if (Math.abs(ew.distanceTraveled -
						_myLocation.distance(ew.fireLocation)) < 50
						&& Math.abs(bulletVelocity(e.getBullet().getPower())
						- ew.bulletVelocity) < 0.001) {
					hitWave = ew;
					break;
				}
			}

			if (hitWave != null) {
				logHit(hitWave, hitBulletLocation);

				// We can remove this wave now, of course.
				_enemyWaves.remove(_enemyWaves.lastIndexOf(hitWave));
			}
		}
	}

	/**
	 * Paint a red circle around our PaintingRobot
	 */
	public void onPaint(Graphics2D g) {
		g.setColor(Color.red);
		g.drawOval((int) (getX() - 50), (int) (getY() - 50), 100, 100);
		g.setColor(new Color(0, 0xFF, 0, 30));
		g.fillOval((int) (getX() - 60), (int) (getY() - 60), 120, 120);
	}

	///////////////////////Helper Methods
	class EnemyWave {
		Point2D.Double fireLocation;
		long fireTime;
		double bulletVelocity, directAngle, distanceTraveled;
		int direction;

		public EnemyWave() { }
	}
	public double wallSmoothing(Point2D.Double botLocation, double angle, int orientation) {
		while (!_fieldRect.contains(project(botLocation, angle, WALL_STICK))) {
			angle += orientation*0.05;
		}
		return angle;
	}

	public static Point2D.Double project(Point2D.Double sourceLocation,
										 double angle, double length) {
		return new Point2D.Double(sourceLocation.x + Math.sin(angle) * length,
				sourceLocation.y + Math.cos(angle) * length);
	}

	public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
		return Math.atan2(target.x - source.x, target.y - source.y);
	}

	public static double limit(double min, double value, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double bulletVelocity(double power) {
		return (20.0 - (3.0*power));
	}

	public static double maxEscapeAngle(double velocity) {
		return Math.asin(8.0/velocity);
	}

	public static void setBackAsFront(AdvancedRobot robot, double goAngle) {
		double angle =
				Utils.normalRelativeAngle(goAngle - robot.getHeadingRadians());
		if (Math.abs(angle) > (Math.PI/2)) {
			if (angle < 0) {
				robot.setTurnRightRadians(Math.PI + angle);
			} else {
				robot.setTurnLeftRadians(Math.PI - angle);
			}
			robot.setBack(100);
		} else {
			if (angle < 0) {
				robot.setTurnLeftRadians(-1*angle);
			} else {
				robot.setTurnRightRadians(angle);
			}
			robot.setAhead(100);
		}
	}
	//Helper method end

	//Surfing the Waves
	public Point2D.Double predictPosition(EnemyWave surfWave, int direction) {
		Point2D.Double predictedPosition = (Point2D.Double)_myLocation.clone();
		double predictedVelocity = getVelocity();
		double predictedHeading = getHeadingRadians();
		double maxTurning, moveAngle, moveDir;

		int counter = 0; // number of ticks in the future
		boolean intercepted = false;

		do {    // the rest of these code comments are rozu's
			moveAngle =
					wallSmoothing(predictedPosition, absoluteBearing(surfWave.fireLocation,
							predictedPosition) + (direction * (Math.PI/2)), direction)
							- predictedHeading;
			moveDir = 1;

			if(Math.cos(moveAngle) < 0) {
				moveAngle += Math.PI;
				moveDir = -1;
			}

			moveAngle = Utils.normalRelativeAngle(moveAngle);

			// maxTurning is built in like this, you can't turn more then this in one tick
			maxTurning = Math.PI/720d*(40d - 3d*Math.abs(predictedVelocity));
			predictedHeading = Utils.normalRelativeAngle(predictedHeading
					+ limit(-maxTurning, moveAngle, maxTurning));

			// this one is nice ;). if predictedVelocity and moveDir have
			// different signs you want to breack down
			// otherwise you want to accelerate (look at the factor "2")
			predictedVelocity +=
					(predictedVelocity * moveDir < 0 ? 2*moveDir : moveDir);
			predictedVelocity = limit(-8, predictedVelocity, 8);

			// calculate the new predicted position
			predictedPosition = project(predictedPosition, predictedHeading,
					predictedVelocity);

			counter++;

			if (predictedPosition.distance(surfWave.fireLocation) <
					surfWave.distanceTraveled + (counter * surfWave.bulletVelocity)
							+ surfWave.bulletVelocity) {
				intercepted = true;
			}
		} while(!intercepted && counter < 500);

		return predictedPosition;
	}

	public void updateWaves() {
		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);

			ew.distanceTraveled = (getTime() - ew.fireTime) * ew.bulletVelocity;
			if (ew.distanceTraveled >
					_myLocation.distance(ew.fireLocation) + 50) {
				_enemyWaves.remove(x);
				x--;
			}
		}
	}
	public double checkDanger(EnemyWave surfWave, int direction) {
		int index = getFactorIndex(surfWave,
				predictPosition(surfWave, direction));

		return _surfStats[index];
	}
	public EnemyWave getClosestSurfableWave() {
		double closestDistance = 50000; // I juse use some very big number here
		EnemyWave surfWave = null;

		for (int x = 0; x < _enemyWaves.size(); x++) {
			EnemyWave ew = (EnemyWave)_enemyWaves.get(x);
			double distance = _myLocation.distance(ew.fireLocation)
					- ew.distanceTraveled;

			if (distance > ew.bulletVelocity && distance < closestDistance) {
				surfWave = ew;
				closestDistance = distance;
			}
		}

		return surfWave;
	}

	public static int getFactorIndex(EnemyWave ew, Point2D.Double targetLocation) {
		double offsetAngle = (absoluteBearing(ew.fireLocation, targetLocation)
				- ew.directAngle);
		double factor = Utils.normalRelativeAngle(offsetAngle)
				/ maxEscapeAngle(ew.bulletVelocity) * ew.direction;

		return (int)limit(0,
				(factor * ((BINS - 1) / 2)) + ((BINS - 1) / 2),
				BINS - 1);
	}
	public void logHit(EnemyWave ew, Point2D.Double targetLocation) {
		int index = getFactorIndex(ew, targetLocation);

		for (int x = 0; x < BINS; x++) {
			// for the spot bin that we were hit on, add 1;
			// for the bins next to it, add 1 / 2;
			// the next one, add 1 / 5; and so on...
			_surfStats[x] += 1.0 / (Math.pow(index - x, 2) + 1);
		}
	}

	public void doSurfing() {
		EnemyWave surfWave = getClosestSurfableWave();

		if (surfWave == null) { return; }

		double dangerLeft = checkDanger(surfWave, -1);
		double dangerRight = checkDanger(surfWave, 1);

		double goAngle = absoluteBearing(surfWave.fireLocation, _myLocation);
		if (dangerLeft < dangerRight) {
			goAngle = wallSmoothing(_myLocation, goAngle - (Math.PI/2), -1);
		} else {
			goAngle = wallSmoothing(_myLocation, goAngle + (Math.PI/2), 1);
		}

		setBackAsFront(this, goAngle);
	}
	//Wave Surfing END


	}



	/**
     * Created by liaquats on 9/26/18.
     */
	/**
	public class WaveBullet
	{
		private double startX, startY, startBearing, power;
		private long   fireTime;
		private int    direction;
		private int[]  returnSegment;

		public WaveBullet(double x, double y, double bearing, double power,
						  int direction, long time, int[] segment)
		{
			startX         = x;
			startY         = y;
			startBearing   = bearing;
			this.power     = power;
			this.direction = direction;
			fireTime       = time;
			returnSegment  = segment;
		}

		public double getBulletSpeed()
		{
			return 20 - power * 3;
		}

		public double maxEscapeAngle()
		{
			return Math.asin(8 / getBulletSpeed());
		}
		public boolean checkHit(double enemyX, double enemyY, long currentTime)
		{
			// if the distance from the wave origin to our enemy has passed
			// the distance the bullet would have traveled...
			if (Point2D.distance(startX, startY, enemyX, enemyY) <=
					(currentTime - fireTime) * getBulletSpeed())
			{
				double desiredDirection = Math.atan2(enemyX - startX, enemyY - startY);
				double angleOffset = Utils.normalRelativeAngle(desiredDirection - startBearing);
				double guessFactor =
						Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
				int index = (int) Math.round((returnSegment.length - 1) /2 * (guessFactor + 1));
				returnSegment[index]++;
				return true;
			}
			return false;
		}
	} // end WaveBullet class
	 **/

