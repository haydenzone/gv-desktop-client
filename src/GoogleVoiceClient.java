import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.techventus.server.voice.Voice;
import com.techventus.server.voice.datatypes.records.*;

public class GoogleVoiceClient {
	public static Voice voice;
	private static Map<String, Notification> currentNotifications = Collections
			.synchronizedMap(new HashMap<String, Notification>());

	public static void main(String[] args) throws InterruptedException, IOException {
		String userName = "";
		String pass = "";
		String userHome = System.getProperty("user.home");  
		String file = userHome + "/.gv";
		File f = new File(file);
		if(f.exists()) { 
			System.out.println("Settings file found");
		    Scanner sc = new Scanner(f);
		    userName = sc.nextLine();
		    pass = sc.nextLine();
		}
		else {
			System.out.println("Settings file not found at "+file);
			// Input the userName and password
			userName = getUsername();
			pass = getPassword();
		}
			


		
		//Attempt to login 
		try {
			voice = new Voice(userName, pass);
		}
		catch (IOException e) {
			// Print out the exception that occurred
			JOptionPane.showMessageDialog(null,
				    "Incorrect username or password. Exiting...",
				    "Google Voice Desktop Client",
				    JOptionPane.ERROR_MESSAGE);
			System.out.println(e.getMessage());
			System.exit(0);
		}
		
		//Loop through threads and create popups for unread messages
		for(SMSThread thread : voice.getSMSThreads()) {
			if (!thread.getRead()) {
				Notification popUp = new Notification(thread);
				popUp.launch();
				currentNotifications.put(thread.getId(), popUp);
			}
		}
		

		// Add an icon to the system tray if supported
		if (!SystemTray.isSupported()) {
			System.out.println("SystemTray is not supported");
			// Create a simple window to kill the application if need be.
			TinyWindow win = new TinyWindow();
			win.setVisible(true);
			win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		} 
		else {
			createSystemTrayIcon();
		}

		// Enter query loop on server
		while (true) {
			
			//Pull down current list of threads from server
			Collection<SMSThread> new_threads = voice.getSMSThreads();
			
			//Loop through all threads
			for (SMSThread thread : new_threads) {
				// Check to see if the thread is read or not
				if (thread.getRead()) { // Is read
					// Check to see if a popup exists
					if (currentNotifications.containsKey(thread.getId())) {
						// Kill popups for this thread
						currentNotifications.get(thread.getId()).dispose();
						removeNotifcationEntry(thread.getId());
					}

				} else { // Is unread
							// Check to see if a popup exists
					if (currentNotifications.containsKey(thread.getId())) {
						// Update the threads pop
						currentNotifications.get(thread.getId())
								.update_thread(thread);
					} else {// No pop up
							// Create the popup
						Notification new_notif = new Notification(thread);
						new_notif.launch();
						currentNotifications.put(thread.getId(), new_notif);
					}

				}
			}
			Thread.sleep(1000);
		}

	}

	public static void createSystemTrayIcon() {
		final PopupMenu popup = new PopupMenu();
		
		
		//Load the icon
		Image image = null;
		try {
			ClassLoader classLoader = Thread.currentThread()
					.getContextClassLoader();
			InputStream input = classLoader
					.getResourceAsStream("gv_small.gif");
			image = ImageIO.read(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Set the icon
		final TrayIcon trayIcon = new TrayIcon(image,
				"Google Voice Desktop Client");
		
		//Get the tray
		final SystemTray tray = SystemTray.getSystemTray();
		// Create a menu for the icon
		MenuItem exitItem = new MenuItem("Exit");
		exitItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				System.exit(0);

			}
		});

		// Add button to menu
		popup.add(exitItem);

		//Register popup menu with trayIcon
		trayIcon.setPopupMenu(popup);

		//Add it to the tray
		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			System.out.println("TrayIcon could not be added.");
		}
		
	}
	public static void removeNotifcationEntry(String threadID) {
		currentNotifications.remove(threadID);
	}

	public static void markMessageAsRead(String id) {
		try {
			voice.markAsRead(id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static boolean sendText(String toNum, String sentMsg)
			throws IOException {
		voice.sendSMS(toNum, sentMsg);
		return true;
	}

	public static String getUsername() {
		return JOptionPane.showInputDialog(null, "UserName", "Google Voice Desktop Client",
				JOptionPane.QUESTION_MESSAGE);
	}
	public static String getPassword() {
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter a password:");
		JPasswordField pass_field = new JPasswordField(10);
		panel.add(label);
		panel.add(pass_field);
		String[] options = new String[] { "OK", "Cancel" };
		JOptionPane.showOptionDialog(null, panel, "Google Voice Desktop Client",
				JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				options, options[1]);
		return String.copyValueOf(pass_field.getPassword());
	}
}

@SuppressWarnings("serial")
class TinyWindow extends JFrame {
	public TinyWindow() {
		Container c = getContentPane();
		c.setLayout(new FlowLayout());
		JButton close = new JButton("Close");
		close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		c.add(close);
		pack();
	}
}
