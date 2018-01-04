import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;


public class VideoPlayer extends JPanel implements ActionListener {

    VideoPlayer(BufferedImage [] img, int fps) {
        this.frameInterval = 1000 / fps;

        this.frameUpdateTimer = new Timer(this.frameInterval, this);
        this.frames = new ImageIcon[img.length];
        for(int i = 0;i < img.length;i++) {
            this.frames[i] = new ImageIcon(img[i]);
        }
    }

    public void playVideo() {
        this.frameIndex = 0;
        JFrame disp = new JFrame("SAR Video Player");
        disp.setLayout(new GridLayout());
        disp.add(this, BorderLayout.NORTH);
        disp.pack();
        disp.setSize(800,800);
        disp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        disp.setVisible(true);
        this.frameUpdateTimer.start();
    }

    public void stopVideo() {
        frameUpdateTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(this.frameIndex < this.frames.length) {
            if (this.frames[this.frameIndex].getImageLoadStatus() == MediaTracker.COMPLETE) {
                this.frames[this.frameIndex].paintIcon(this, g, 0, 0);
                this.frameIndex++;
            }
        }
        else {
            frameUpdateTimer.stop();
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }

    private int frameIndex;
    private int frameInterval;
    Timer frameUpdateTimer;
    private ImageIcon [] frames;
}
