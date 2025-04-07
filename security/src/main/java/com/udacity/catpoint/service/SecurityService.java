package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.image.ImageService;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class SecurityService {
    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners = new CopyOnWriteArraySet<>();
    private boolean catDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    public void setArmingStatus(ArmingStatus armingStatus) {
        // Always reset alarm when disarming
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Reset all sensors and handle cat detection
            resetAllSensors();

            // Special case: Arming to HOME with cat detected
            if (armingStatus == ArmingStatus.ARMED_HOME && catDetected) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }

        securityRepository.setArmingStatus(armingStatus);
        notifyArmingStatusChanged(armingStatus); // Add this line
    }

    public void resetAllSensors() {
        new HashSet<>(getSensors()).forEach(sensor -> {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
        });
    }

    public void processImage(BufferedImage image) {
        if (image == null) {
            // Only notify listeners without changing alarm status
            catDetected = false;
            statusListeners.forEach(sl -> sl.catDetected(false));
            return;
        }

        boolean currentCatDetected = imageService.imageContainsCat(image);
        this.catDetected = currentCatDetected;
        statusListeners.forEach(sl -> sl.catDetected(currentCatDetected));

        if (currentCatDetected && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!currentCatDetected && allSensorsInactive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // Block changes if alarm is active
        if (getAlarmStatus() == AlarmStatus.ALARM) return;

        boolean wasActive = sensor.getActive();
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        handleSensorStateChange(active, wasActive);
    }

    public void handleSensorStateChange(boolean active, boolean wasActive) {
        AlarmStatus status = getAlarmStatus();
        ArmingStatus arming = getArmingStatus();

        if (active) {
            if (arming != ArmingStatus.DISARMED) {
                if (status == AlarmStatus.NO_ALARM) {
                    setAlarmStatus(AlarmStatus.PENDING_ALARM);
                } else if (status == AlarmStatus.PENDING_ALARM) {
                    setAlarmStatus(AlarmStatus.ALARM);
                }
            }
        } else {
            if (status == AlarmStatus.PENDING_ALARM && allSensorsInactive()) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
    }

    // Add missing notifiers
    public void notifyCatDetection(boolean detected) {
        statusListeners.forEach(sl -> sl.catDetected(detected));
    }

    public void notifyArmingStatusChanged(ArmingStatus status) {
        statusListeners.forEach(sl -> sl.sensorStatusChanged());
    }

    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    public boolean areAnySensorsActive() {
        return getSensors().stream().anyMatch(Sensor::getActive);
    }

    public boolean allSensorsInactive() {
        return !areAnySensorsActive();
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
    public Set<StatusListener> getStatusListeners() {
        return new HashSet<>(statusListeners);
    }
}