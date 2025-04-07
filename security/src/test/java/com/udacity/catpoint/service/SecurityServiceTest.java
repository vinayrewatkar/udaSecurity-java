package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityServiceTest {

    private SecurityService securityService;
    private TestSecurityRepository securityRepository;
    private TestImageService imageService;
    private TestStatusListener statusListener;

    private Sensor sensor;
    private BufferedImage catImage;
    private BufferedImage nonCatImage;

    @BeforeEach
    void init() {
        securityRepository = new TestSecurityRepository();
        imageService = new TestImageService();
        statusListener = new TestStatusListener();

        securityService = new SecurityService(securityRepository, imageService);
        securityService.addStatusListener(statusListener);

        sensor = new Sensor("Test Sensor", SensorType.DOOR);
        catImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        nonCatImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    }


    // ========== setArmingStatus() Tests ==========
    @Test
    void setArmingStatus_disarmed_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void setArmingStatus_armed_resetsAllSensors(ArmingStatus status) {
        Sensor activeSensor = new Sensor("Active", SensorType.WINDOW);
        activeSensor.setActive(true);
        securityService.addSensor(activeSensor);

        securityService.setArmingStatus(status);

        assertFalse(activeSensor.getActive());
        assertEquals(status, securityService.getArmingStatus());
    }

    @Test
    void setArmingStatus_armedHomeWithCat_setsAlarm() {
        imageService.setContainsCat(true);
        securityService.processImage(catImage);
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    // ========== processImage() Tests ==========
    @Test
    void processImage_catDetectedWhileArmedHome_setsAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        imageService.setContainsCat(true);
        securityService.processImage(catImage);
        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void processImage_noCatWithInactiveSensors_setsNoAlarm() {
        securityService.addSensor(sensor);
        imageService.setContainsCat(false);
        securityService.processImage(nonCatImage);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void processImage_nullImage_notifiesNoCat() {
        securityService.processImage(null);
        assertFalse(statusListener.isCatDetected());
    }

    // ========== changeSensorActivationStatus() Tests ==========
    @Test
    void changeSensorActivationStatus_activeWhileArmed_setsPendingAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.changeSensorActivationStatus(sensor, true);
        assertEquals(AlarmStatus.PENDING_ALARM, securityService.getAlarmStatus());
    }

    @Test
    void changeSensorActivationStatus_secondActiveWhilePending_setsAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        Sensor sensor2 = new Sensor("Sensor 2", SensorType.WINDOW);
        securityService.addSensor(sensor);
        securityService.addSensor(sensor2);

        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor2, true);

        assertEquals(AlarmStatus.ALARM, securityService.getAlarmStatus());
    }

    @Test
    void changeSensorActivationStatus_allInactiveWhilePending_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.addSensor(sensor);
        securityService.changeSensorActivationStatus(sensor, true);
        securityService.changeSensorActivationStatus(sensor, false);
        assertEquals(AlarmStatus.NO_ALARM, securityService.getAlarmStatus());
    }

    // ========== Sensor Management Tests ==========
    @Test
    void addSensor_addsToRepository() {
        securityService.addSensor(sensor);
        assertTrue(securityService.getSensors().contains(sensor));
    }

    @Test
    void removeSensor_removesFromRepository() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
        assertFalse(securityService.getSensors().contains(sensor));
    }

    // ========== Status Listener Tests ==========
    @Test
    void addStatusListener_addsListener() {
        TestStatusListener newListener = new TestStatusListener();
        securityService.addStatusListener(newListener);
        assertTrue(securityService.getStatusListeners().contains(newListener));
    }

    @Test
    void removeStatusListener_removesListener() {
        securityService.removeStatusListener(statusListener);
        assertFalse(securityService.getStatusListeners().contains(statusListener));
    }

    @Test
    void getStatusListeners_returnsCopy() {
        Set<StatusListener> listeners = securityService.getStatusListeners();
        listeners.clear();
        assertFalse(securityService.getStatusListeners().isEmpty());
    }

    // ========== State Check Tests ==========
    @Test
    void areAnySensorsActive_returnsTrueWhenActive() {
        sensor.setActive(true);
        securityService.addSensor(sensor);
        assertTrue(securityService.areAnySensorsActive());
    }

    @Test
    void allSensorsInactive_returnsTrueWhenNoneActive() {
        securityService.addSensor(sensor);
        assertTrue(securityService.allSensorsInactive());
    }

    // ========== Notification Tests ==========
    @Test
    void notifyCatDetection_notifiesAllListeners() {
        TestStatusListener listener2 = new TestStatusListener();
        securityService.addStatusListener(listener2);

        securityService.notifyCatDetection(true);

        assertTrue(statusListener.isCatDetected());
        assertTrue(listener2.isCatDetected());
    }

    @Test
    void notifyArmingStatusChanged_notifiesAllListeners() {
        TestStatusListener listener2 = new TestStatusListener();
        securityService.addStatusListener(listener2);

        securityService.notifyArmingStatusChanged(ArmingStatus.ARMED_HOME);

        assertNotNull(statusListener.getLastSensorStatusChange());
        assertNotNull(listener2.getLastSensorStatusChange());
    }

    // ========== Helper Classes ==========
    private static class TestImageService implements ImageService {
        private boolean containsCat = false;

        public void setContainsCat(boolean containsCat) {
            this.containsCat = containsCat;
        }

        @Override
        public boolean imageContainsCat(BufferedImage image) {
            return containsCat;
        }
    }

    private static class TestStatusListener implements StatusListener {
        private AlarmStatus lastStatus;
        private boolean catDetected;
        private Long lastSensorStatusChange;

        @Override
        public void notify(AlarmStatus status) {
            this.lastStatus = status;
        }

        @Override
        public void catDetected(boolean catDetected) {
            this.catDetected = catDetected;
        }

        @Override
        public void sensorStatusChanged() {
            this.lastSensorStatusChange = System.currentTimeMillis();
        }

        public AlarmStatus getLastStatus() {
            return lastStatus;
        }

        public boolean isCatDetected() {
            return catDetected;
        }

        public Long getLastSensorStatusChange() {
            return lastSensorStatusChange;
        }
    }

    private static class TestSecurityRepository implements SecurityRepository {
        private AlarmStatus alarmStatus = AlarmStatus.NO_ALARM;
        private ArmingStatus armingStatus = ArmingStatus.DISARMED;
        private Set<Sensor> sensors = new HashSet<>();

        @Override
        public void addSensor(Sensor sensor) {
            sensors.add(sensor);
        }

        @Override
        public void removeSensor(Sensor sensor) {
            sensors.remove(sensor);
        }

        @Override
        public void updateSensor(Sensor sensor) {
            if (sensors.contains(sensor)) {
                sensors.remove(sensor);
                sensors.add(sensor);
            }
        }

        @Override
        public void setAlarmStatus(AlarmStatus alarmStatus) {
            this.alarmStatus = alarmStatus;
        }

        @Override
        public AlarmStatus getAlarmStatus() {
            return alarmStatus;
        }

        @Override
        public void setArmingStatus(ArmingStatus armingStatus) {
            this.armingStatus = armingStatus;
            if (armingStatus == ArmingStatus.DISARMED) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
            if (armingStatus == ArmingStatus.ARMED_HOME || armingStatus == ArmingStatus.ARMED_AWAY) {
                sensors.forEach(sensor -> sensor.setActive(false));
            }
        }

        @Override
        public ArmingStatus getArmingStatus() {
            return armingStatus;
        }

        @Override
        public Set<Sensor> getSensors() {
            return new HashSet<>(sensors);
        }
    }
}