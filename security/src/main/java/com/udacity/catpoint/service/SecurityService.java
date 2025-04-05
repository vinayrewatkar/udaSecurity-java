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

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private ImageService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean catDetected = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */
    public void setArmingStatus(ArmingStatus armingStatus) {
        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Reset all sensors to inactive when system is armed
            getSensors().forEach(sensor -> {
                sensor.setActive(false);
                securityRepository.updateSensor(sensor);
            });

            // If cat was detected and we're arming to ARMED_HOME, set the alarm status
            if(catDetected && armingStatus == ArmingStatus.ARMED_HOME) {
                setAlarmStatus(AlarmStatus.ALARM);
            }
        }
        securityRepository.setArmingStatus(armingStatus);
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catDetected = cat;
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if(!cat && !areAnySensorsActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return; //no problem if the system is disarmed
        }
        switch(securityRepository.getAlarmStatus()) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated() {
        if(securityRepository.getAlarmStatus() != AlarmStatus.ALARM &&
                !areAnySensorsActive()) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
        // If alarm is active, don't change status regardless of sensor state changes
        boolean wasActive = sensor.getActive();

        // Update the sensor's state
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        // Get current system status
        AlarmStatus currentStatus = securityRepository.getAlarmStatus();

        // If alarm is active, sensor state should not affect the alarm state
        if (currentStatus == AlarmStatus.ALARM) {
            return;
        }

        // If system pending alarm and this sensor becomes active or was already active and is reactivated
        if (currentStatus == AlarmStatus.PENDING_ALARM) {
            if (active) {
                securityRepository.setAlarmStatus(AlarmStatus.ALARM);
                return;
            } else {
                // Check if all sensors are now inactive
                boolean allSensorsInactive = true;
                for (Sensor s : securityRepository.getSensors()) {
                    if (s.getActive()) {
                        allSensorsInactive = false;
                        break;
                    }
                }

                // If all sensors are inactive, set to no alarm
                if (allSensorsInactive) {
                    securityRepository.setAlarmStatus(AlarmStatus.NO_ALARM);
                    return;
                }
            }
        }

        // If sensor becomes active while system is armed and not in pending/alarm state
        if (!wasActive && active &&
                (securityRepository.getArmingStatus() == ArmingStatus.ARMED_HOME ||
                        securityRepository.getArmingStatus() == ArmingStatus.ARMED_AWAY) &&
                currentStatus == AlarmStatus.NO_ALARM) {
            securityRepository.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        }
    }

    /**
     * Send an image to the SecurityService for processing. The securityService will use its provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage));
    }

    /**
     * Check if any sensors are currently active.
     * @return true if any sensors are active, false otherwise
     */
    private boolean areAnySensorsActive() {
        return getSensors().stream().anyMatch(Sensor::getActive);
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