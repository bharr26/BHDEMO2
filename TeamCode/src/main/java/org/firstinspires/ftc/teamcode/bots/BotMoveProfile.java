package org.firstinspires.ftc.teamcode.bots;

import android.graphics.Point;
import android.nfc.Tag;
import android.util.Log;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.calibration.BotCalibConfig;
import org.firstinspires.ftc.teamcode.calibration.MotorReductionBot;
import org.firstinspires.ftc.teamcode.gamefield.FieldStats;
import org.firstinspires.ftc.teamcode.odometry.IBaseOdometry;
import org.firstinspires.ftc.teamcode.skills.Geometry;
import org.opencv.core.Mat;

public class BotMoveProfile {
    private double realSpeedLeft = 0;
    private double realSpeedRight = 0;
    private double longTarget = 0;
    private double shortTarget = 0;
    private double longTargetBack = 0;
    private double shortTargetBack = 0;
    private double rightTarget = 0;
    private double leftTarget = 0;
    private double rightTargetBack = 0;
    private double leftTargetBack = 0;
    private double slowdownMarkLong = 0;
    private double slowdownMarkShort = 0;
    private boolean leftLong;
    private double speedRatio = 1;
    private double topSpeed = 0;
    private double lowSpeed = 0;
    private RobotDirection direction;
    private MotorReductionBot motorReduction = new MotorReductionBot();
    private BotMoveRequest target = new BotMoveRequest();
    private MoveStrategy strategy = MoveStrategy.Curve;
    private MoveStrategy nextStep = null;

    private double currentHead = 0;
    private double targetVector = 0;
    private double angleChange = 0;

    public static double DEFAULT_HEADING = -1;
    private double desiredHead = DEFAULT_HEADING;

    private double distanceRatio = 1;
    private double distance = 0;

    private Point start;
    private Point destination;
    private Point actual;

    private double initialSpeed = 0;
    private double speedIncrement = 0.05;
    private double minSpeed = 0.2;
    private double minSpeedSpin = 0.1;
    private double speedDecrement = 0.05;

    private boolean continuous = false;
    private boolean dryRun = true;

    private DcMotor.ZeroPowerBehavior zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE;

    public static double MOTOR_WHEEL_OFFSET = 1.25;
    public static double ERROR_MARGIN_INCHES = 2;
    public static final String TAG = "BotMoveProfile";

    public BotMoveProfile(){

    }

    public BotMoveProfile(MoveStrategy strategy){
        super();
        this.setStrategy(strategy);
    }

    @Override
    public String toString() {
        int actualX = 0;
        int actualY = 0;
        if (actual != null){
            actualX = actual.x;
            actualY = actual.y;
        }
        destination = this.getTarget().getTarget();
        return String.format("Long Target: %.2f Direction: %s \nL:%.2f  R:%.2f\n Slowdowns: %.2f %.2f\nHead: %.2f Target: %.2f Change: %.2f\nFrom: %d %d\nTo: %d %d\nActual: %d %d\nSpeedR: %.2f Dist R: %.2f", longTarget, direction.name(), realSpeedLeft, realSpeedRight, slowdownMarkLong,slowdownMarkShort, currentHead, targetVector, angleChange, start.x, start.y, destination.x, destination.y, actualX, actualY, speedRatio, distanceRatio);
    }

    public double getRealSpeedLeft() {
        return realSpeedLeft;
    }

    public void setRealSpeedLeft(double realSpeedLeft) {
        this.realSpeedLeft = realSpeedLeft;
    }

    public double getRealSpeedRight() {
        return realSpeedRight;
    }

    public void setRealSpeedRight(double realSpeedRight) {
        this.realSpeedRight = realSpeedRight;
    }

    public double getLongTarget() {
        return longTarget;
    }

    public void setLongTarget(double longTarget) {
        this.longTarget = longTarget;
    }

    public double getSlowdownMarkLong() {
        return slowdownMarkLong;
    }

    public void setSlowdownMarkLong(double slowdownMarkLong) {
        this.slowdownMarkLong = slowdownMarkLong;
    }

    public double getSlowdownMarkShort() {
        return slowdownMarkShort;
    }

    public void setSlowdownMarkShort(double slowdownMarkShort) {
        this.slowdownMarkShort = slowdownMarkShort;
    }

    public boolean isLeftLong() {
        return leftLong;
    }

    public void setLeftLong(boolean leftLong) {
        this.leftLong = leftLong;
    }

    public MotorReductionBot getMotorReduction() {
        return motorReduction;
    }

    public void setMotorReduction(MotorReductionBot motorReduction) {
        this.motorReduction = motorReduction;
    }

    public RobotDirection getDirection() {
        return direction;
    }

    public void setDirection(RobotDirection direction) {
        this.direction = direction;
    }

    public double getSpeedRatio() {
        return speedRatio;
    }

    public void setSpeedRatio(double speedRatio) {
        this.speedRatio = speedRatio;
    }

    public BotMoveRequest getTarget() {
        return target;
    }

    public void setTarget(BotMoveRequest target) {
        this.target = target;
    }

    public double getCurrentHead() {
        return currentHead;
    }

    public void setCurrentHead(double currentHead) {
        this.currentHead = currentHead;
    }

    public double getTargetVector() {
        return targetVector;
    }

    public void setTargetVector(double targetVector) {
        this.targetVector = targetVector;
    }

    public double getAngleChange() {
        return angleChange;
    }

    public void setAngleChange(double angleChange) {
        this.angleChange = angleChange;
    }

    public Point getStart() {
        return start;
    }

    public void setStart(Point start) {
        this.start = start;
    }

    public Point getDestination() {
        return destination;
    }

    public void setDestination(Point destination) {
        this.destination = destination;
    }

    public Point getActual() {
        return actual;
    }

    public void setActual(Point actual) {
        this.actual = actual;
    }

    public static BotMoveProfile bestRoute(IOdoBot bot, double currentX, double currentY, Point target, RobotDirection direction, double topSpeed, MoveStrategy preferredStrategy, double desiredHead, IBaseOdometry locator){
        if (topSpeed == 0){
            return null;
        }

        Point currentPos = new Point((int)currentX, (int)currentY);

        //if AutoLine, set the ideal destination
        if (preferredStrategy == MoveStrategy.AutoLine){
            if (target.x == 0){
                target.x = (int)currentX;
            }

            if (target.y == 0){
                target.y = (int)currentY;
            }
        }

        double currentHead = locator.getAdjustedCurrentHeading();
        Log.d(TAG, String.format("currentHead: %.2f", currentHead));

        if (direction == RobotDirection.Backward) {
            currentHead = (currentHead + 180) % 360;
        }

        double distance = Geometry.getDistance(currentX, currentY, target.x, target.y);
        Log.d(TAG, String.format("Distance: %.2f", distance));


        //do not move if within margin of error, unless spinning
        if (preferredStrategy == MoveStrategy.None || (Math.abs(distance) <= ERROR_MARGIN_INCHES && preferredStrategy != MoveStrategy.Spin)){
            return null;
        }

        //determine the new heading to the target
        double distanceX = target.x - currentX;
        double distanceY = target.y - currentY;
        //locator maybe off within 1 inch
        if (distanceX > -1 && distanceX < 1){
            distanceX = 0;
        }
        if (distanceY > -1 && distanceY < 1){
            distanceY = 0;
        }
        //////
        double targetVectorLocal = Math.toDegrees(Math.atan2(Math.abs(distanceY), Math.abs(distanceX)));
        double targetVector = targetVectorLocal;
        Log.d(TAG, String.format("Distance X: %.2f, Distance Y: %.2f", distanceX, distanceY));
        Log.d(TAG, String.format("targetVectorLocal: %.2f", targetVectorLocal));

        if (distanceY == 0){
            if (distanceX > 0){
                targetVector = 270;
            }
            else{
                targetVector = 90;
            }
        }
        else if (distanceX == 0){
            if(distanceY < 0){
                targetVector = 180;
            }
            else{
                targetVector = 0;
            }
        }
        else{
            //lower left
            if (distanceX < 0 && distanceY < 0){
                targetVector = targetVectorLocal + 90;
            }
            //lower right
            if (distanceX > 0 && distanceY < 0){
                targetVector =  270 - targetVectorLocal;
            }
            //upper right
            if (distanceX > 0 && distanceY > 0){
                targetVector = 270 + targetVectorLocal;
            }
            //upper left
            if (distanceX < 0 && distanceY > 0){
                targetVector = 90 - targetVectorLocal;
            }
        }

        Log.d(TAG, String.format("targetVector adjusted: %.2f", targetVector));
        double realAngleChange = Geometry.getAngle(targetVector, currentHead);
        //locator heading may be off within 1 degree.
        if (realAngleChange > -1 && realAngleChange < 1){
            realAngleChange = 0;
        }
        ///////////////
        double angleChange = Math.abs(realAngleChange);
        Log.d(TAG, String.format("angleChange: %.2f, realchange: %.2f", angleChange, realAngleChange));


        if (angleChange > 90){
            if (direction == RobotDirection.Optimal) {
                //better go backwards
                currentHead = (currentHead + 180) % 360;
                realAngleChange = Geometry.getAngle(targetVector, currentHead);
                if (realAngleChange > -1 && realAngleChange < 1){
                    realAngleChange = 0;
                }
                angleChange = Math.abs(realAngleChange);
                direction = RobotDirection.Backward;
                Log.d(TAG, String.format("Go backwards. angleChange: %.2f, realchange: %.2f", angleChange, realAngleChange));
            }
            else {
                //spin
                Log.d(TAG, String.format("Spinning before curve"));
                return buildSpinProfile(bot, realAngleChange, topSpeed, currentPos, direction, MoveStrategy.Curve);
            }
        }

        if (direction == RobotDirection.Optimal){
            direction = RobotDirection.Forward;
        }

        if (preferredStrategy == MoveStrategy.AutoLine){
            double sign = Math.signum(realAngleChange);
            double diagDistance = distance / Math.cos(Math.toRadians(Math.abs(45-angleChange)));
            MoveStrategy toLineStrategy = MoveStrategy.Diag;
            Log.d(TAG, String.format("AutoLine angleChange = %.2f", angleChange));
            Log.d(TAG, String.format("AutoLine diagDistance = %.2f", diagDistance));
            if (angleChange > 45){
                distance = distance / Math.cos(Math.toRadians(Math.abs(90 - angleChange)));
                Log.d(TAG, String.format("AutoLine strafeDistance = %.2f", distance));
                if (distance <= diagDistance){
                    toLineStrategy = MoveStrategy.Strafe;
                }
            }
            else{
                distance = distance / Math.cos(Math.toRadians(angleChange));
                Log.d(TAG, String.format("AutoLine straightDistance = %.2f", distance));
                if (distance <= diagDistance){
                    toLineStrategy = MoveStrategy.Straight;
                }
            }
            if (toLineStrategy == MoveStrategy.Strafe){
                //strafe
                realAngleChange = 90 * sign;
                return buildStrafeProfile(bot.getCalibConfig(), realAngleChange, topSpeed, distance, direction, target, currentHead, targetVector, locator, null);
            }
            else if (toLineStrategy == MoveStrategy.Straight){
                //straight
                return buildMoveProfile(bot, distance, topSpeed, 0, 0, false, direction, target, currentHead, currentHead, locator);
            }
            else{
                //diag
                realAngleChange = 45 * sign;
                return  buildDiagRawProfile(bot, bot.getCalibConfig(), realAngleChange, topSpeed, diagDistance, currentPos, target, direction,null);
            }
        }

        if(preferredStrategy == MoveStrategy.SpinNCurve ){
            return buildSpinProfile(bot, realAngleChange, topSpeed, currentPos, direction, MoveStrategy.Curve);
        }

        if(preferredStrategy == MoveStrategy.SpinNStraight){
            return buildSpinProfile(bot, realAngleChange, topSpeed, currentPos, direction, MoveStrategy.Straight);
        }

        if (preferredStrategy == MoveStrategy.Spin){
            return getFinalHeadProfile(bot, desiredHead, topSpeed, locator);
        }

        if (preferredStrategy == MoveStrategy.Strafe){
            return buildStrafeProfile(bot.getCalibConfig(), realAngleChange, topSpeed, distance, direction, target, currentHead, targetVector, locator, MoveStrategy.Straight);
        }

        if (preferredStrategy == MoveStrategy.Diag){
            return  buildDiagProfile(bot, bot.getCalibConfig(), realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Straight);
        }

        if (preferredStrategy == MoveStrategy.Curve && angleChange >= 42 && angleChange <= 48){
            Log.d(TAG, String.format("Diagonal is better than curve"));
            return  buildDiagProfile(bot, bot.getCalibConfig(), realAngleChange, topSpeed, distance, currentPos, target, direction,null);
        }

        double chord = distance;

        boolean reduceLeft = false;

        //turing left when moving forward
        if (realAngleChange > 0) {
            reduceLeft = true;
        }

        if (direction == RobotDirection.Backward){
            reduceLeft = !reduceLeft;
        }

        //check radius.
        double radius = Geometry.getRadius(angleChange, chord);

        //Auto mode
        if (preferredStrategy == MoveStrategy.Auto){
            return buildAutoProfile(bot, chord, realAngleChange, topSpeed, distance, direction, currentPos, target, currentHead, targetVector, desiredHead, radius, reduceLeft, locator, MoveStrategy.Straight);
        }

        //straight
        if (preferredStrategy == MoveStrategy.Straight){
            return buildMoveProfile(bot, distance, topSpeed, 0, 0, reduceLeft, direction, target, currentHead, targetVector, locator);
        }

        //curve
        if ((reduceLeft && radius <= bot.getCalibConfig().getMinRadiusLeft()) ||
                (reduceLeft == false && radius <= bot.getCalibConfig().getMinRadiusRight())) {
            Log.d(TAG, String.format("Radius %.2f is too small to curve. Will strafe or diagonal", radius));
            if (Math.abs(realAngleChange) < 45) {
                return buildDiagProfile(bot, bot.getCalibConfig(), realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Straight);
            }
            else{
                return buildStrafeProfile(bot.getCalibConfig(), realAngleChange, topSpeed, distance, direction, target, currentHead, targetVector, locator, MoveStrategy.Straight);
            }
        }

        BotMoveProfile profile = buildMoveProfile(bot, chord, topSpeed, radius, angleChange, reduceLeft, direction, target, currentHead, targetVector, locator);

        return profile;
    }

    private static BotMoveProfile buildAutoProfile(IOdoBot bot, double chord, double realAngleChange, double topSpeed, double distance, RobotDirection direction, Point currentPos, Point target, double currentHead, double targetVector, double desiredHead, double radius, boolean reduceLeft, IBaseOdometry locator, MoveStrategy next){

        BotCalibConfig botConfig = bot.getCalibConfig();

        double angleChange = Math.abs(realAngleChange);
        //consider desired head
        //if current = desired, favor strafe or diag

        boolean keepHead = false;
        if (desiredHead != DEFAULT_HEADING) {
            double headChange = Math.abs(Geometry.getAngle(desiredHead, currentHead));

            if (headChange <= 10) {
                keepHead = true;
            }
        }

        //if distance is within 2x robot dimensions, diag or strafe
        if (distance <= FieldStats.ROBOT_SIDE * 2){
            if (angleChange >= 48){
                return buildStrafeProfile(botConfig, realAngleChange, topSpeed, distance, direction, target, currentHead, targetVector, locator, MoveStrategy.Straight);
            }
            else{
                return buildDiagProfile(bot, botConfig, realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Straight);
            }
        }


        /////////////
        //longer distance
        // important to preserve orientation
        if (keepHead){
            if (angleChange <= 45) {
                return buildDiagProfile(bot, botConfig, realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Straight);
            }
            else{
                if (Math.abs(angleChange) <= 75){
                    return buildDiagProfile(bot, botConfig, realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Strafe);
                }
                else {
                    return buildStrafeProfile(botConfig, realAngleChange, topSpeed, distance, direction, target, currentHead, targetVector, locator, MoveStrategy.Straight);
                }
            }
        }
        else{
            //orientation does not matter
            //diag
            if (angleChange >= 42 && angleChange <= 48){
                return  buildDiagProfile(bot, bot.getCalibConfig(), realAngleChange, topSpeed, distance, currentPos, target, direction, MoveStrategy.Straight);
            }

            //curve backwards does not work very well, hence spin and straight
            // and if the radius is too small
            if (direction == RobotDirection.Backward ||  (reduceLeft && radius <= botConfig.getMinRadiusLeft()) ||
                    (reduceLeft == false && radius <= botConfig.getMinRadiusRight())) {
                return  buildSpinProfile(bot, angleChange, topSpeed, currentPos, direction, MoveStrategy.Curve);
            }
            else{
                //curve
                return buildMoveProfile(bot, chord, topSpeed, radius, angleChange, reduceLeft, direction, target, currentHead, targetVector, locator);
            }
        }
    }

    public static BotMoveProfile buildMoveProfile(IOdoBot bot, double chord, double topSpeed, double radius, double angleChange, boolean reduceLeft, RobotDirection direction, Point target, double currentHead, double targetVector, IBaseOdometry locator){
        BotMoveProfile profile = new BotMoveProfile();

        double longArch = chord;
        double shortArch = chord;
        double speedRatio = 1;
        double lowSpeed = topSpeed;
        BotCalibConfig botConfig = bot.getCalibConfig();

        if (angleChange > 0) {
            double theta = Math.toRadians(angleChange * 2);
            //double centerArch = theta * radius;
            double wheelDistFromCenter = botConfig.getWheelBaseSeparation() / 2;
            longArch = theta * (radius + wheelDistFromCenter);
            shortArch = theta * (radius - wheelDistFromCenter);
            //for speed, the ratio is greater as the spread between the motorized wheels is wider by MOTOR_WHEEL_OFFSET on each side
            double longArchMotor = longArch + MOTOR_WHEEL_OFFSET;
            double shortArchMotor = shortArch - MOTOR_WHEEL_OFFSET;
            speedRatio = shortArchMotor / longArchMotor;
            lowSpeed = topSpeed * speedRatio;
        }
        else {
            profile.setStrategy(MoveStrategy.Straight);
        }

        profile.setSpeedRatio(speedRatio);
        profile.setDistanceRatio(shortArch/longArch);

        double leftSpeed, rightSpeed;
        if (reduceLeft) {
            leftSpeed = lowSpeed;
            rightSpeed = topSpeed;
        } else {
            leftSpeed = topSpeed;
            rightSpeed = lowSpeed;
        }

        if (direction == RobotDirection.Backward){
            shortArch = -shortArch;
            longArch = -longArch;
        }
        else{
            rightSpeed = -rightSpeed;
            leftSpeed = -leftSpeed;
        }


        double distanceLong = longArch * bot.getEncoderCountsPerInch()*bot.getEncoderDirection();
        double distanceShort = shortArch * bot.getEncoderCountsPerInch()*bot.getEncoderDirection();

        boolean leftLong = true;
//        double startingPointLong = 0, startingPointShort = 0, startingPointLongBack = 0, startingPointShortBack = 0;


        if (leftSpeed >= rightSpeed) {
//            startingPointLong = bot.getLeftOdometer();
//            startingPointLongBack = bot.getLeftBackOdometer();
//            startingPointShort = bot.getRightOdometer();
//            startingPointShortBack = bot.getRightBackOdometer();
        } else if (rightSpeed > leftSpeed) {
            leftLong = false;
//            startingPointShort = bot.getLeftOdometer();
//            startingPointShortBack = bot.getLeftBackOdometer();
//            startingPointLong = bot.getRightOdometer();
//            startingPointLongBack = bot.getRightBackOdometer();

        }

        double averagePower = (Math.abs(rightSpeed) + Math.abs(leftSpeed))/2;
        averagePower = Math.round(averagePower*10)/10.0;

        MotorReductionBot mr = botConfig.getMoveMRForward();
        if (direction == RobotDirection.Backward) {
            mr = botConfig.getMoveMRBack();
        }
        double breakPoint = mr.getBreakPoint(averagePower);

        double sign = Math.signum(distanceLong);

        //account for processing time
//        double ticksLong = YellowBot.MAX_VELOCITY_PER_PROC_DELAY*topSpeed;
//        double ticksShort = YellowBot.MAX_VELOCITY_PER_PROC_DELAY*lowSpeed;

        double slowdownMarkLong = sign*(Math.abs(distanceLong) - breakPoint);
        double slowdownMarkShort = sign*(Math.abs(distanceShort) - breakPoint);

        double longTarget = sign*(Math.abs(distanceLong));
        double longTargetBack = sign*(Math.abs(distanceLong));
        double shortTarget = sign*(Math.abs(distanceShort));
        double shortTargetBack = sign*(Math.abs(distanceShort));


        if (leftLong){
            profile.setLeftTarget(longTarget);
            profile.setLeftTargetBack(longTargetBack);
            profile.setRightTarget(shortTarget);
            profile.setRightTargetBack(shortTargetBack);
        }
        else{
            profile.setLeftTarget(shortTarget);
            profile.setLeftTargetBack(shortTargetBack);
            profile.setRightTarget(longTarget);
            profile.setRightTargetBack(longTargetBack);
        }

        profile.setLeftLong(leftLong);
        profile.setSlowdownMarkLong(slowdownMarkLong);
        profile.setSlowdownMarkShort(slowdownMarkShort);
        profile.setLongTarget(longTarget);
        profile.setShortTarget(shortTarget);
        profile.setLongTargetBack(longTargetBack);
        profile.setShortTargetBack(shortTargetBack);
        profile.setRealSpeedLeft(leftSpeed);
        profile.setRealSpeedRight(rightSpeed);
        profile.setMotorReduction(mr);
        profile.setDirection(direction);
        BotMoveRequest rq = new BotMoveRequest();
        rq.setTarget(target);
        rq.setTopSpeed(topSpeed);
        rq.setDirection(direction);
        rq.setMotorReduction(profile.getMotorReduction());
        profile.setTarget(rq);
        profile.setStart(new Point((int)locator.getCurrentX(), (int)locator.getCurrentY()));
        profile.setAngleChange(angleChange);
        profile.setCurrentHead(currentHead);
        profile.setTargetVector(targetVector);
        return profile;
    }

    private static BotMoveProfile buildSpinProfile(IOdoBot bot, double angleChange, double topSpeed, Point currentPos, RobotDirection direction, MoveStrategy next){
        BotMoveProfile profile = new BotMoveProfile();
        profile.setAngleChange(angleChange);
        profile.setStrategy(MoveStrategy.Spin);
        profile.setTopSpeed(topSpeed);
        profile.setRealSpeedLeft(topSpeed);
        profile.setRealSpeedRight(topSpeed);
//        if(next != null && next == MoveStrategy.Straight){
//            if (Math.abs(profile.getAngleChange()) < 30){
//                profile.setTopSpeed(0.1);
//            }
//            else{
//                profile.setTopSpeed(0.2);
//            }
//        }
        profile.setNextStep(next);
        profile.setStart(currentPos);

        BotMoveRequest rq = new BotMoveRequest();
        rq.setTarget(currentPos);
        rq.setTopSpeed(topSpeed);
        rq.setDirection(direction);
        rq.setMotorReduction(profile.getMotorReduction());
        profile.setTarget(rq);
        profile.setDirection(direction);

        boolean spinLeft = false;
        if (angleChange > 0) {
            spinLeft = true;
        }
        double ticksLeft = Math.abs(bot.getCalibConfig().getLeftTicksPerDegree()*angleChange);
        double ticksRight = Math.abs(bot.getCalibConfig().getRightTicksPerDegree()*angleChange);
        if (spinLeft){
            ticksLeft = -ticksLeft;
        }
        else{
            ticksRight = -ticksRight;
        }
        profile.setLeftTarget(ticksLeft*bot.getEncoderDirection());
        profile.setRightTarget(ticksRight*bot.getEncoderDirection());
        profile.setLeftTargetBack(ticksLeft*bot.getEncoderDirection());
        profile.setRightTargetBack(ticksRight*bot.getEncoderDirection());

        Log.d(TAG, String.format("buildSpinProfile. Angle change: %.2f  spinLeft: %b  ticksLeft: %.2f  ticksRight: %.2f ", angleChange, spinLeft, ticksLeft, ticksRight));


        return profile;
    }

    public static BotMoveProfile buildStrafeProfile(BotCalibConfig botConfig, double angleChange, double topSpeed, double distance, RobotDirection direction, Point target, double currentHead, double targetVector, IBaseOdometry locator, MoveStrategy next){
        double strafeDistance = distance * Math.sin(Math.toRadians(Math.abs(angleChange)));

        return buildStrafeRawProfile(botConfig, angleChange, topSpeed, strafeDistance, direction, target, currentHead, targetVector, locator, next);
    }

    private static BotMoveProfile buildStrafeRawProfile(BotCalibConfig botConfig, double angleChange, double topSpeed, double strafeDistance, RobotDirection direction, Point target, double currentHead, double targetVector, IBaseOdometry locator, MoveStrategy next){
        BotMoveProfile profile = new BotMoveProfile();

        if (direction == RobotDirection.Backward){
            angleChange = -angleChange;
        }

        boolean left = angleChange > 0;
        if (left){
            profile.setMotorReduction(botConfig.getStrafeLeftReduction());
        }
        else{
            profile.setMotorReduction(botConfig.getStrafeRightReduction());
        }
        profile.setDistance(strafeDistance);
        profile.setLongTarget(strafeDistance* YellowBot.COUNTS_PER_INCH_REV);
        profile.setAngleChange(angleChange);
        profile.setStrategy(MoveStrategy.Strafe);
        profile.setTopSpeed(topSpeed);
        profile.setDirection(RobotDirection.Optimal);
        BotMoveRequest rq = new BotMoveRequest();
        rq.setTarget(target);
        rq.setTopSpeed(topSpeed);
        rq.setDirection(RobotDirection.Optimal);
        rq.setMotorReduction(profile.getMotorReduction());
        profile.setTarget(rq);
        profile.setStart(new Point((int)locator.getCurrentX(), (int)locator.getCurrentY()));
        profile.setAngleChange(angleChange);
        profile.setCurrentHead(currentHead);
        profile.setTargetVector(targetVector);
        profile.setNextStep(next);
        return profile;
    }

    private static BotMoveProfile buildDiagProfile(IOdoBot bot, BotCalibConfig botConfig, double angleChange, double topSpeed, double distance, Point currentPos, Point target, RobotDirection direction, MoveStrategy next){
        double strafeDistance = distance * Math.sin(Math.toRadians(Math.abs(angleChange)));
        double diagDistance = strafeDistance/Math.cos(Math.toRadians(45));

       return buildDiagRawProfile(bot, botConfig, angleChange, topSpeed, diagDistance, currentPos, target, direction, next);
    }

    private static BotMoveProfile buildDiagRawProfile(IOdoBot bot, BotCalibConfig botConfig, double angleChange, double topSpeed, double distance, Point currentPos, Point target, RobotDirection direction, MoveStrategy next){
        BotMoveProfile profile = new BotMoveProfile();

        boolean left = angleChange > 0;
        if (left){
            profile.setMotorReduction(botConfig.getDiagMRLeft());
        }
        else{
            profile.setMotorReduction(botConfig.getDiagMRRight());
        }
        double distanceTicks = Math.abs(distance * bot.getEncoderCountsPerInch());
        if (direction == RobotDirection.Backward){
            distanceTicks = - distanceTicks;
        }
        distanceTicks = distanceTicks * bot.getEncoderDirection();

        distanceTicks = distanceTicks / 0.66; // Math.sin(Math.toRadians(45));

        profile.setLeftTarget(distanceTicks);
        profile.setRightTarget(distanceTicks);
        profile.setLeftTargetBack(distanceTicks);
        profile.setRightTargetBack(distanceTicks);

        profile.setDistance(distance);
        profile.setLongTarget(distanceTicks);
        profile.setAngleChange(angleChange);
        profile.setStrategy(MoveStrategy.Diag);
        profile.setTopSpeed(topSpeed);
        profile.setDirection(direction);
        profile.setNextStep(next);

        BotMoveRequest rq = new BotMoveRequest();
        rq.setTarget(target);
        rq.setTopSpeed(topSpeed);
        rq.setDirection(direction);
        rq.setMotorReduction(profile.getMotorReduction());
        profile.setTarget(rq);
        profile.setStart(currentPos);

        return profile;
    }

    public static BotMoveProfile getFinalHeadProfile(IOdoBot bot, double desiredHeading, double speed, IBaseOdometry locator){
        BotMoveProfile profileSpin = null;
        if (desiredHeading != BotMoveProfile.DEFAULT_HEADING) {
            Point currentPos = new Point((int)locator.getCurrentX(), (int)locator.getCurrentY());
            double currentHead = locator.getAdjustedCurrentHeading();
            double realAngleChange = Geometry.getAngle(desiredHeading, currentHead);
            profileSpin = BotMoveProfile.buildSpinProfile(bot,realAngleChange, speed, currentPos, RobotDirection.Optimal, null);
        }
        return profileSpin;
    }

    public MoveStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(MoveStrategy strategy) {
        this.strategy = strategy;
    }

    public double getDistanceRatio() {
        return distanceRatio;
    }

    public void setDistanceRatio(double distanceRatio) {
        this.distanceRatio = distanceRatio;
    }

    public double getTopSpeed() {
        return topSpeed;
    }

    public void setTopSpeed(double topSpeed) {
        this.topSpeed = topSpeed;
    }

    public MoveStrategy getNextStep() {
        return nextStep;
    }

    public void setNextStep(MoveStrategy nextStep) {
        this.nextStep = nextStep;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getDesiredHead() {
        return desiredHead;
    }

    public void setDesiredHead(double desiredHead) {
        this.desiredHead = desiredHead;
    }

    public double getLowSpeed() {
        return lowSpeed;
    }

    public void setLowSpeed(double lowSpeed) {
        this.lowSpeed = lowSpeed;
    }

    public double getInitialSpeed() {
        return initialSpeed;
    }

    public void setInitialSpeed(double initialSpeed) {
        this.initialSpeed = initialSpeed;
    }

    public double getSpeedIncrement() {
        return speedIncrement;
    }

    public void setSpeedIncrement(double speedIncrement) {
        this.speedIncrement = speedIncrement;
    }

    public void disableAcceleration(){
        this.initialSpeed = this.topSpeed;
    }

    public double getMinSpeed() {
        return minSpeed;
    }

    public void setMinSpeed(double minSpeed) {
        this.minSpeed = minSpeed;
    }

    public double getSpeedDecrement() {
        return speedDecrement;
    }

    public void setSpeedDecrement(double speedDecrement) {
        this.speedDecrement = speedDecrement;
    }

    public DcMotor.ZeroPowerBehavior getZeroPowerBehavior() {
        return zeroPowerBehavior;
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior zeroPowerBehavior) {
        this.zeroPowerBehavior = zeroPowerBehavior;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous = continuous;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean shouldStop(){
        return !isContinuous() || isDryRun();
    }

    public double getTopVelocity() {
        return this.getTopSpeed() * YellowBot.MAX_VELOCITY;
    }

    public double getLowVelocity() {
        return getLowSpeed() * YellowBot.MAX_VELOCITY;
    }

    public double getMinSpeedSpin() {
        return minSpeedSpin;
    }

    public void setMinSpeedSpin(double minSpeedSpin) {
        this.minSpeedSpin = minSpeedSpin;
    }

    public double getShortTarget() {
        return shortTarget;
    }

    public void setShortTarget(double shortTarget) {
        this.shortTarget = shortTarget;
    }

    public double getRightTarget() {
        return rightTarget;
    }

    public void setRightTarget(double rightTarget) {
        this.rightTarget = rightTarget;
    }

    public double getLeftTarget() {
        return leftTarget;
    }

    public void setLeftTarget(double leftTarget) {
        this.leftTarget = leftTarget;
    }

    public double getRightTargetBack() {
        return rightTargetBack;
    }

    public void setRightTargetBack(double rightTargetBack) {
        this.rightTargetBack = rightTargetBack;
    }

    public double getLeftTargetBack() {
        return leftTargetBack;
    }

    public void setLeftTargetBack(double leftTargetBack) {
        this.leftTargetBack = leftTargetBack;
    }

    public double getLongTargetBack() {
        return longTargetBack;
    }

    public void setLongTargetBack(double longTargetBack) {
        this.longTargetBack = longTargetBack;
    }

    public double getShortTargetBack() {
        return shortTargetBack;
    }

    public void setShortTargetBack(double shortTargetBack) {
        this.shortTargetBack = shortTargetBack;
    }
}
