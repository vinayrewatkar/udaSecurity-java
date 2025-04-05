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
        // Handle disarmed case first
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
            securityRepository.setArmingStatus(armingStatus);
            return;
        }

        // Handle armed cases
        // Reset all sensors when arming
        Set<Sensor> sensorsCopy = new HashSet<>(getSensors());
        sensorsCopy.forEach(sensor -> {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
        });

        // Special case for armed-home with cat detected
        if (armingStatus == ArmingStatus.ARMED_HOME && catDetected) {
            setAlarmStatus(AlarmStatus.ALARM);
        }

        securityRepository.setArmingStatus(armingStatus);
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // If alarm is active, ignore all sensor changes
        if (securityRepository.getAlarmStatus() == AlarmStatus.ALARM) {
            return;
        }

        boolean wasActive = sensor.getActive();
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        AlarmStatus alarmStatus = securityRepository.getAlarmStatus();
        ArmingStatus armingStatus = securityRepository.getArmingStatus();

        // Handle sensor activation
        if (active) {
            handleSensorActivated(armingStatus, alarmStatus);
        } else {
            handleSensorDeactivated(alarmStatus);
        }
    }

    private void handleSensorActivated(ArmingStatus armingStatus, AlarmStatus alarmStatus) {
        if (armingStatus == ArmingStatus.DISARMED) {
            return;
        }

        switch (alarmStatus) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    private void handleSensorDeactivated(AlarmStatus alarmStatus) {
        if (alarmStatus == AlarmStatus.PENDING_ALARM && !areAnySensorsActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    public void processImage(BufferedImage image) {
        boolean currentCatDetected = image != null && imageService.imageContainsCat(image);
        this.catDetected = currentCatDetected;
        notifyCatDetection(currentCatDetected);

        // Handle alarm status based on cat detection
        if (currentCatDetected && securityRepository.getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!currentCatDetected && !areAnySensorsActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    private void notifyCatDetection(boolean catDetected) {
        statusListeners.forEach(sl -> sl.catDetected(catDetected));
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

    private boolean areAnySensorsActive() {
        return getSensors().stream().anyMatch(Sensor::getActive);
    }

    private boolean allSensorsInactive() {
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
}