package frc.robot.subsystems;

import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.AbsoluteSensorRangeValue;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import com.revrobotics.CANSparkMax;
import com.revrobotics.RelativeEncoder;
import com.revrobotics.CANSparkBase.IdleMode;
import com.revrobotics.CANSparkLowLevel.MotorType;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.Constants;
import frc.robot.Constants.SwerveContants;

public class SwerveModule {
    private final CANSparkMax driveMotor;
    private final CANSparkMax turningMotor;

    private final RelativeEncoder driveMotorEncoder;
    private final RelativeEncoder turningMotEncoder;

    private final CANcoder turningAbsoluteEncoder;

    private final PIDController turningPidController;

    public SwerveModule(int driverMotorID,int turningMotorID,int turningAbsoluteEncoderID,double absoluteEncoderOffset){
        driveMotor = new CANSparkMax(driverMotorID,MotorType.kBrushless);
        turningMotor = new CANSparkMax(turningMotorID,MotorType.kBrushless);

        driveMotorEncoder = driveMotor.getEncoder();
        turningMotEncoder = turningMotor.getEncoder();

        turningAbsoluteEncoder = new CANcoder(turningAbsoluteEncoderID);

        turningPidController = new PIDController(SwerveContants.turningPidController_Kp,SwerveContants.turningPidController_Ki,SwerveContants.turningPidController_Kd);
        turningPidController.enableContinuousInput(SwerveContants.pidRangeMin,SwerveContants.pidRangeMin);

        driveMotor.restoreFactoryDefaults();
        turningMotor.restoreFactoryDefaults();

        turningMotor.setInverted(SwerveContants.turningMotorInversion);
        driveMotor.setInverted(SwerveContants.driveMotorInversion);

        driveMotor.setIdleMode(IdleMode.kCoast);
        turningMotor.setIdleMode(IdleMode.kCoast);

        var cancoderCfg = new CANcoderConfiguration();
        cancoderCfg.MagnetSensor.AbsoluteSensorRange = AbsoluteSensorRangeValue.Signed_PlusMinusHalf;
        cancoderCfg.MagnetSensor.SensorDirection = SensorDirectionValue.Clockwise_Positive;
        cancoderCfg.MagnetSensor.MagnetOffset = absoluteEncoderOffset;
        turningAbsoluteEncoder.getConfigurator().apply(cancoderCfg);

        driveMotorEncoder.setVelocityConversionFactor(SwerveContants.driveVelocityConversionFactor);
        driveMotorEncoder.setPositionConversionFactor(SwerveContants.drivePositionConversionFactor);
    }
    public SwerveModuleState getstate(){
        return new SwerveModuleState(driveMotorEncoder.getVelocity(),Rotation2d.fromDegrees(turningAbsoluteEncoder.getAbsolutePosition().getValue()));
    }

    public SwerveModulePosition getPosition(){
        return new SwerveModulePosition(driveMotorEncoder.getPosition(),Rotation2d.fromDegrees(turningAbsoluteEncoder.getAbsolutePosition().getValue()));
    }
    public void setState(SwerveModuleState state){
        SwerveModuleState optimizedState = SwerveModuleState.optimize(state,getstate().angle);
        double turningMotorOutput = turningPidController.calculate(getstate().angle.getDegrees(),optimizedState.angle.getDegrees());
        turningMotor.set(turningMotorOutput);
        driveMotor.set(optimizedState.speedMetersPerSecond);
    }

    
}
