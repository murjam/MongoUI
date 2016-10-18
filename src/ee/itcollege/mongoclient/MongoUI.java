package ee.itcollege.mongoclient;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;

public class MongoUI implements Runnable {

	private MongoClient mongo;

	private JsonWriterSettings jsonFormat = new JsonWriterSettings(JsonMode.STRICT, "  ", "\n");

	@Override
	public void run() {

		mongo = new MongoClient("localhost:27017");

		JFrame window = new JFrame("Magical MongoDB client");

		window.setLayout(new BorderLayout());

		JTextArea command = new JTextArea(20, 50);
		JTextArea feedback = new JTextArea(30, 50);

		Font font = new Font(Font.MONOSPACED, Font.BOLD, 12);
		command.setFont(font);
		feedback.setFont(font);

		command.setBackground(new Color(200, 200, 200));
		JPanel buttonPanel = new JPanel();
		JButton submit = new JButton("submit command");
		submit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					feedback.setText("");
					String c = command.getText();
					String[] split = c.split("[\\.\\(]");
					if (split.length < 3 || !"db".equals(split[0])) {
						feedback.setText("Start the command with 'db.collectionName.command'");
						System.out.println(Arrays.toString(split));
						return;
					}
					String collectionName = split[1];
					String commandName = split[2];

					String json = c.substring(c.indexOf('(') + 1, c.lastIndexOf(')'));

					System.out.println(json);

					MongoCollection<Document> collection = mongo.getDatabase("local").getCollection(collectionName);

					switch (commandName) {
					case "find":
						FindIterable<Document> find;
						if (json.isEmpty()) {
							find = collection.find();
						} else {
							find = collection.find(BasicDBObject.parse(json));
						}
						int count = 0;
						StringBuilder res = new StringBuilder();
						for (Document document : find) {
							count++;
							res.append(document.toJson(jsonFormat) + "\n");
						}
						feedback.setText(String.format("Found %d documents\n\n", count) + res.toString());
						break;

					case "insertOne":
						collection.insertOne(Document.parse(json));
						feedback.setText("Well, it seemes that the insert was successful.\n"
								+ "You can now select the items with: " + String.format("db.%s.find()", collection));
						break;

					case "deleteOne":
					case "deleteMany":
						DeleteResult delRes;
						if ("deleteOne".equals(commandName)) {
							delRes = collection.deleteOne(Document.parse(json));
						} else {
							delRes = collection.deleteMany(Document.parse(json));
						}
						feedback.setText(String.format("%d rows deleted.", delRes.getDeletedCount()));
						break;

					default:
						feedback.setText(String.format("Command '%s' not implemented", commandName));
						break;
					}
				} catch (Exception ex) {
					feedback.setText(ex.getMessage());
					ex.printStackTrace();
				}
			}
		});
		buttonPanel.add(submit);

		window.add(command, BorderLayout.WEST);
		window.add(buttonPanel, BorderLayout.EAST);
		window.add(feedback, BorderLayout.SOUTH);

		window.pack();

		window.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				mongo.close();
			}
		});

		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setVisible(true);
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new MongoUI());
	}

}
