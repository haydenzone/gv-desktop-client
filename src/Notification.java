import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Random;
import javax.swing.*;
import com.techventus.server.voice.datatypes.records.SMS;
import com.techventus.server.voice.datatypes.records.SMSThread;

@SuppressWarnings("serial")
public class Notification extends JFrame implements Runnable {
	private SMSThread thread;
	private JTextArea message_box;
	private JTextArea message_input;

	public void run() {
		setVisible(true);
	}

	public static String getMostRecentMessage(SMSThread thread) {
		Collection<SMS> messages = thread.getAllSMS();
		String message = messages.iterator().next().getContent();
		return message;
	}

	public Notification(SMSThread toShow) {
		thread = toShow;
		createPopUp(toShow.getContact().getName(),
				Notification.getMostRecentMessage(toShow));
	}

	public void update_thread(SMSThread newThread) {
		thread = newThread;
		message_box.setText(getMostRecentMessage(newThread));

	}

	public void createPopUp(String from, String message) {
		setMaximumSize(new Dimension(300, 100));
		setAlwaysOnTop(true);
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		Container north = new Container();
		north.setLayout(new FlowLayout());

		// Display who the message is from
		north.add(new JLabel("From: " + from));
		c.setBackground(Color.GREEN);
		c.add(north, BorderLayout.NORTH);
		message_box = new JTextArea(6, 20);
		message_box.setLineWrap(true);
		message_box.setEditable(false);
		message_box.setText(message);
		c.add(message_box, BorderLayout.CENTER);

		// Create the reply section
		JPanel reply_area = new JPanel();
		reply_area.setLayout(new FlowLayout());
		message_input = new JTextArea(6, 20);
		message_input.setLineWrap(true);
		JScrollPane scrollable_input = new JScrollPane(message_input);
		JButton reply_button = new JButton("Reply");
		ActionListener send = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		};
		message_input.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
					send();
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
			}
		});
		reply_button.addActionListener(send);
		reply_area.add(scrollable_input);
		reply_area.add(reply_button);
		c.add(reply_area, BorderLayout.SOUTH);

		// Register a close listener
		WindowListener exitListener = new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				GoogleVoiceClient.markMessageAsRead(thread.getId());
				GoogleVoiceClient.removeNotifcationEntry(thread.getId());
				dispose();
			}
		};
		addWindowListener(exitListener);
		pack();
		// Select the window location
		Random gen = new Random(System.currentTimeMillis());
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension windowSize = getSize();
		int x_lower_bound = (int) (screenSize.getWidth()
				- windowSize.getWidth() - 200);
		int windowX = Math.max(
				0,
				x_lower_bound
						+ gen.nextInt(screenSize.width - windowSize.width
								- x_lower_bound));
		int windowY = Math.max(0,
				gen.nextInt(screenSize.height - windowSize.height));
		setLocation(windowX, windowY);
	}

	public void launch() {
		Thread t = new Thread(this, "My Thread");
		t.start();

	}

	public void send() {
		String toSend = message_input.getText();
		if (valid_message(toSend)) {
			System.out.println("Sending message \"" + toSend + "\" to "
					+ thread.getContact().getNumber());

			this.dispose();
			try {
				GoogleVoiceClient.sendText(thread.getContact().getNumber(), toSend);
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
			}
			GoogleVoiceClient.removeNotifcationEntry(thread.getId());
		}

	}

	private boolean valid_message(String message) {
		if (message.equals("")) {
			return false;
		}
		return true;
	}

	public static void main(String[] args) {

	}

}
