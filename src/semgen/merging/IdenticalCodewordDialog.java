package semgen.merging;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class IdenticalCodewordDialog extends JDialog implements PropertyChangeListener, ItemListener {

	private static final long serialVersionUID = 7836229234967611648L;
	public JOptionPane optionPane;
	public JTextField mantextfield = new JTextField();
	public JTextArea area = new JTextArea();
	public JComboBox<String> box;
	public Boolean process = true;

	public IdenticalCodewordDialog(String cdwd, File file1, File file2) {
		String[] selections = new String[] { "Rename codeword in " + file2.getName(),
				"Only keep " + cdwd + " from " + file1.getName(),
				"Only keep " + cdwd + " from " + file2.getName()};
		area.setEditable(false);

		area.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
		area.setText("Duplicate codeword: " + cdwd
				+ "\nChoose a resolution option:"
				+ "\n(This change will only appear in the merged model)");
		area.setLineWrap(true);
		area.setWrapStyleWord(true);
		area.setOpaque(false);

		box = new JComboBox<String>(selections);
		box.addItemListener(this);
		mantextfield.setForeground(Color.blue);
		JPanel mainpanel = new JPanel(new BorderLayout());
		mainpanel.add(area, BorderLayout.NORTH);
		mainpanel.add(box, BorderLayout.CENTER);
		mainpanel.add(mantextfield, BorderLayout.SOUTH);

		setModal(true);
		this.setTitle("Duplicate codeword: " + cdwd);

		Object[] array = new Object[] { mainpanel };
		Object[] options = new Object[] { "OK", "Cancel" };

		optionPane = new JOptionPane(array, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, null);
		optionPane.addPropertyChangeListener(this);
		optionPane.setOptions(options);
		optionPane.setInitialValue(options[0]);

		setContentPane(optionPane);

		pack();
		setVisible(true);
	}

	public void propertyChange(PropertyChangeEvent e) {
		String value = optionPane.getValue().toString();
		if (value == "OK") {
			if (box.getSelectedIndex() == 0) {

				if (!mantextfield.getText().equals("")
						&& mantextfield.getText() != null) {
					System.out.println(mantextfield.getText());
					setVisible(false);
				} else {
					System.out.println("Please enter a new codeword name");
				}
			} else {
				setVisible(false);
			}
		} else if (value == "Cancel") {
			process = false;
			setVisible(false);
		}
		optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
	}

	public void itemStateChanged(ItemEvent arg0) {
		mantextfield.setEnabled(!(box.getSelectedIndex() > 0));
	}
}
