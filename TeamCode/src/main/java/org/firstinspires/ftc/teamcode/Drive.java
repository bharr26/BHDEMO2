package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.commandftc.opModes.CommandBasedTeleOp;
import org.firstinspires.ftc.teamcode.commands.arm.RotateArmCommand;
import org.firstinspires.ftc.teamcode.commands.drive.ArcadeDriveCommand;
import org.firstinspires.ftc.teamcode.commands.drive.DriveLeftCommand;
import org.firstinspires.ftc.teamcode.commands.drive.DriveRightCommand;
import org.firstinspires.ftc.teamcode.commands.drive.TankDriveCommand;
import org.firstinspires.ftc.teamcode.commands.lift.MoveLiftCommand;
import org.firstinspires.ftc.teamcode.subsystems.ArmSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.DriveTrainSubsystem;
import org.firstinspires.ftc.teamcode.subsystems.LiftSubsystem;

@TeleOp(name = "Drive")
public class Drive extends CommandBasedTeleOp
{
    DriveTrainSubsystem driveTrain;
    LiftSubsystem liftSubsystem;
    ArmSubsystem armSubsystem;
    // Drive Commands
    TankDriveCommand tankDriveCommand;
    ArcadeDriveCommand arcadeDriveCommand;
    DriveLeftCommand driveLeftCommand;
    DriveRightCommand driveRightCommand;
    // Lift Commands
    MoveLiftCommand raiseLift;
    MoveLiftCommand lowerLift;
    // Arm commands
    RotateArmCommand rotateArmCommand;

    private double getDriveSpeed() {
        if (gamepad1.left_trigger > 0)          return 0.5;
        else if (gamepad1.right_trigger > 0)    return 1;
        else                                    return 0.75;
    }

    @Override
    public void assign() {
        driveTrain = new DriveTrainSubsystem();
        liftSubsystem = new LiftSubsystem();
        armSubsystem = new ArmSubsystem();

        tankDriveCommand = new TankDriveCommand(driveTrain, () -> -gamepad1.left_stick_y * getDriveSpeed(), () -> -gamepad1.right_stick_y * getDriveSpeed());
        arcadeDriveCommand = new ArcadeDriveCommand(driveTrain, () -> gamepad1.left_stick_x, () -> -gamepad1.left_stick_y, () -> gamepad1.right_stick_x);
        driveLeftCommand = new DriveLeftCommand(driveTrain, this::getDriveSpeed);
        driveRightCommand = new DriveRightCommand(driveTrain, this::getDriveSpeed);

        raiseLift = new MoveLiftCommand(liftSubsystem, 0.3, 0.05);
        lowerLift = new MoveLiftCommand(liftSubsystem, 0.3, -0.05);

        rotateArmCommand = new RotateArmCommand(armSubsystem,
                () -> (gamepad2.left_trigger > gamepad2.right_trigger ?
                        gamepad2.left_trigger : -gamepad2.right_trigger) * 0.1);

        // DriveTrain commands
        driveTrain.setDefaultCommand(tankDriveCommand);
        gp1.x().whileHeld(arcadeDriveCommand);
        gp1.dpad_left().whileHeld(driveLeftCommand);
        gp1.left_bumper().whileHeld(driveLeftCommand);
        gp1.dpad_right().whileHeld(driveRightCommand);
        gp1.right_bumper().whileHeld(driveRightCommand);
        // Lift commands
        gp2.dpad_up().whenHeld(raiseLift);
        gp2.dpad_down().whenHeld(lowerLift);
        // Arm command
        armSubsystem.setDefaultCommand(rotateArmCommand);

        // Telemetry
        // No need for anything but update in loop because use of suppliers

        gp1.left_bumper().whenPressed(() -> armSubsystem.setVerticalPosition(armSubsystem.getVerticalPosition() + 0.1), armSubsystem);
        gp1.right_bumper().whenPressed(() -> armSubsystem.setVerticalPosition(armSubsystem.getVerticalPosition() - 0.1), armSubsystem);

        telemetry.addData("Runtime", this::getRuntime);
        telemetry.addData("arm vertical position", armSubsystem::getVerticalPosition);
        telemetry.update();
    }
}