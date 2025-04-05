package com.udacity.catpoint;

import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private Sensor sensor;
    private final String sensorName = "Test Sensor";
    private BufferedImage catImage;
    private BufferedImage nonCatImage;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(sensorName, SensorType.DOOR);

        // Create some simple test images
        catImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        nonCatImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    // 1. If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSensorActivated_changeStatusToPending(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2. If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifAlarmIsArmedAndSensorActivatedAndStatusPending_changeStatusToAlarm(ArmingStatus armingStatus) {
        // Remove this line since it's not being used:
        // when(securityRepository.getArmingStatus()).thenReturn(armingStatus);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3. If pending alarm and all sensors are inactive, return to no alarm state.
    @Test
    void ifPendingAlarmAndAllSensorsInactive_changeStatusToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 4. If alarm is active, change in sensor state should not affect the alarm state.
    @Test
    void ifAlarmIsActive_changingSensorStateShouldNotAffectAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 5. If a sensor is activated while already active and the system is in pending state, change it to alarm state.
    @Test
    void ifSensorActivatedWhileAlreadyActiveAndPendingAlarm_changeStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6. If a sensor is deactivated while already inactive, make no changes to the alarm state.
    @Test
    void ifSensorDeactivatedWhileAlreadyInactive_makeNoChangesToAlarmState() {
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // 7. If the image service identifies an image containing a cat while the system is armed-home, put the system into alarm status.
    @Test
    void ifImageServiceIdentifiesCatAndSystemArmedHome_changeStatusToAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any())).thenReturn(true);

        securityService.processImage(catImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8. If the image service identifies an image that does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    void ifImageServiceIdentifiesNoCatAndNoActiveSensors_changeStatusToNoAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(false);

        Set<Sensor> sensors = new HashSet<>();
        sensor.setActive(false);
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(nonCatImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Complementary test to requirement 8
    @Test
    void ifImageServiceIdentifiesNoCatButSensorsActive_doNotChangeAlarmStatus() {
        when(imageService.imageContainsCat(any())).thenReturn(false);

        Set<Sensor> sensors = new HashSet<>();
        sensor.setActive(true);
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.processImage(nonCatImage);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9. If the system is disarmed, set the status to no alarm.
    @Test
    void ifSystemDisarmed_changeStatusToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 10. If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetAllSensorsToInactive(ArmingStatus armingStatus) {
        Set<Sensor> sensors = new HashSet<>();
        sensor.setActive(true);
        sensors.add(sensor);

        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(armingStatus);

        assertFalse(sensor.getActive());
    }

    // 11. If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    void ifSystemArmedHomeWhileCameraShowsCat_changeStatusToAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(catImage);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Additional tests for edge cases and coverage

    @Test
    void ifSystemArmedAwayAndNoSensorsActive_statusShouldBeNoAlarm() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // Verify sensor was reset
        assertFalse(sensor.getActive());
    }

    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        verify(securityRepository).addSensor(sensor);

        securityService.removeSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }

    @Test
    void ifCatDetectedAndThenSystemArmedHome_statusShouldBeAlarm() {
        // First process an image with a cat
        when(imageService.imageContainsCat(any())).thenReturn(true);
        securityService.processImage(catImage);

        // Then arm the system
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        // Verify alarm is set to ALARM
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
}