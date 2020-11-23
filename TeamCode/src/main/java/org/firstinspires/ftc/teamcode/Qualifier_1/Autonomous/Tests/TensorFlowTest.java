/**
 * Tests Tensor Flow. If Tensor Flow detects
 * 4 rings, the robot should move forward.
 * If it detects 1 ring, the robot should
 * move left. Otherwise, the robot should move
 * right.
 *
 * @author  Aamod
 * @version 1.0
 * @since   2020-November-5
 * @status: Not fully working
 */

package org.firstinspires.ftc.teamcode.Qualifier_1.Autonomous.Tests;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Qualifier_1.Robot;
import org.firstinspires.ftc.teamcode.Qualifier_1.Components.ObjectDetection.TensorFlow;

@Autonomous
//@Disabled
public class TensorFlowTest extends LinearOpMode {

    @Override
    public void runOpMode() {
        Robot robot = new Robot(this);

        robot.initTensorFlow();
        robot.runTensorFlow();
        int rings = robot.tensorFlow.getNumberOfRings();
        telemetry.addData("Nummber of Rings: ", rings);
        telemetry.update();
        sleep(2000);
        robot.stopTensorFlow();

        waitForStart();

        if (rings == 4) {
            robot.moveForward(10,0.5);
        } else if (rings == 1) {
            robot.moveLeft(10,0.5);
        } else {
            robot.moveRight(10, 0.5);
        }
    }
}
