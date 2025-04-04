package com.udacity.catpoint.application;

import com.udacity.catpoint.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImagePanel extends JPanel {
    private SecurityService securityService;

    private JLabel cameraHeader;
    private JLabel cameraLabel;
    private BufferedImage currentCameraImage;

    private int IMAGE_WIDTH = 300;
    private int IMAGE_HEIGHT = 225;

    public ImagePanel(SecurityService securityService) {
        this.securityService = securityService;
        setLayout(new MigLayout());

        cameraHeader = new JLabel("Camera Feed");
        cameraLabel = new JLabel();
        cameraLabel.setBackground(Color.WHITE);
        cameraLabel.setPreferredSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));
        cameraLabel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        JButton addPictureButton = new JButton("Refresh Camera");
        addPictureButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Choose Picture");
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }

            try {
                currentCameraImage = ImageIO.read(chooser.getSelectedFile());
                Image tmp = new ImageIcon(currentCameraImage).getImage();
                cameraLabel.setIcon(new ImageIcon(tmp.getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_SMOOTH)));

                securityService.processImage(currentCameraImage);

            } catch (IOException ioe) {
                JOptionPane.showMessageDialog(null, "Invalid image selected");
            }
        });

        add(cameraHeader, "wrap");
        add(cameraLabel, "wrap");
        add(addPictureButton);
    }
}