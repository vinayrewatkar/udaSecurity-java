package com.udacity.catpoint;

import com.udacity.catpoint.application.StatusListener;
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

    @Mock
    private StatusListener statusListener;

    private Sensor sensor;
    private BufferedImage catImage;
    private BufferedImage nonCatImage;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        securityService.addStatusListener(statusListener);

        sensor = new Sensor("Test Sensor", SensorType.DOOR);
        catImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        nonCatImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }

    // ========== Sensor Activation Tests ==========
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void sensorActivation_whileArmed_changesToPendingAlarm(ArmingStatus status) {
        when(securityRepository.getArmingStatus()).thenReturn(status);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(statusListener).notify(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void secondSensorActivation_whilePending_changesToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).notify(AlarmStatus.ALARM);
    }

    @Test
    void sensorActivation_whileAlarmActive_doesNothing() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // ========== Sensor Deactivation Tests ==========
    @Test
    void allSensorsDeactivated_whilePending_returnsToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void sensorDeactivation_whileAlarmActive_doesNothing() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    @Test
    void sensorDeactivation_whileInactive_doesNothing() {
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // ========== Image Processing Tests ==========
    @Test
    void catImage_whileArmedHome_triggersAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(catImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
        verify(statusListener).catDetected(true);
    }

    @Test
    void catImage_whileDisarmed_doesNotTriggerAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(catImage);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void nonCatImage_withInactiveSensors_setsNoAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.processImage(nonCatImage);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(statusListener).catDetected(false);
    }

    @Test
    void nonCatImage_withActiveSensors_doesNotChangeAlarm() {
        when(imageService.imageContainsCat(any())).thenReturn(false);
        sensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.processImage(nonCatImage);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    // ========== Arming Status Tests ==========
    @Test
    void disarmingSystem_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armingSystem_resetsAllSensors(ArmingStatus status) {
        Sensor activeSensor = new Sensor("Active", SensorType.WINDOW);
        activeSensor.setActive(true);
        when(securityRepository.getSensors()).thenReturn(Set.of(activeSensor));

        securityService.setArmingStatus(status);

        assertFalse(activeSensor.getActive());
        verify(securityRepository).updateSensor(activeSensor);
    }

    @Test
    void armingHome_withCatDetected_immediatelyAlarms() {
        when(imageService.imageContainsCat(any())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        // First detect cat while disarmed
        securityService.processImage(catImage);

        // Then arm to home
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // ========== Listener Tests ==========
    @Test
    void multipleListeners_notifiedProperly() {
        StatusListener secondListener = mock(StatusListener.class);
        securityService.addStatusListener(secondListener);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(statusListener).notify(AlarmStatus.ALARM);
        verify(secondListener).notify(AlarmStatus.ALARM);
    }

    @Test
    void listenerRemoval_worksCorrectly() {
        securityService.removeStatusListener(statusListener);

        securityService.setAlarmStatus(AlarmStatus.ALARM);

        verify(statusListener, never()).notify(any());
    }

    // ========== Edge Cases ==========
    @Test
    void noSensors_doesNotCauseExceptions() {
        when(securityRepository.getSensors()).thenReturn(new HashSet<>());

        assertDoesNotThrow(() -> {
            securityService.changeSensorActivationStatus(sensor, true);
            securityService.processImage(catImage);
            securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        });
    }

    @Test
    void sensorActivation_whileDisarmed_doesNothing() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
    }
    // Test for ARMED_HOME + cat detection branch
    @Test
    void setArmingStatus_armedHomeWithCat_immediatelyAlarms() {
        when(imageService.imageContainsCat(any())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test for allSensorsInactive() case
    @Test
    void changeSensorActivationStatus_allSensorsInactive_returnsToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));
        sensor.setActive(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
}