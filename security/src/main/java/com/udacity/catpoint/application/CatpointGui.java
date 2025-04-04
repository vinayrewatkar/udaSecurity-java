package com.udacity.catpoint.application;

import com.udacity.catpoint.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.image.FakeImageService;
import com.udacity.catpoint.image.ImageService;
import com.udacity.catpoint.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * This is the primary JFrame for the application that contains all the top-level panels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as needed.
 */
public class CatpointGui extends JFrame {
    public CatpointGui() {
        setLocation(100, 100);
        setSize(600, 850);
        setTitle("Very Secure App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());

        SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
        ImageService imageService = new FakeImageService();
        SecurityService securityService = new SecurityService(securityRepository, imageService);

        DisplayPanel displayPanel = new DisplayPanel(securityService);
        ControlPanel controlPanel = new ControlPanel(securityService);
        SensorPanel sensorPanel = new SensorPanel(securityService);
        ImagePanel imagePanel = new ImagePanel(securityService);

        mainPanel.add(displayPanel, "wrap");
        mainPanel.add(imagePanel, "wrap");
        mainPanel.add(controlPanel, "wrap");
        mainPanel.add(sensorPanel);

        getContentPane().add(mainPanel);
    }
}