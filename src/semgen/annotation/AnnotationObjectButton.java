package semgen.annotation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import semgen.annotation.composites.PropertyMarker;
import semgen.resource.SemGenFont;
import semsim.Annotatable;
import semsim.model.SemSimComponent;

public class AnnotationObjectButton extends JPanel implements MouseListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public AnnotatorTab annotater;
	public SemSimComponent ssc;
	public Boolean compannfilled;
	public String companntext;
	public Boolean noncompannfilled;
	public Boolean humdeffilled;
	public Boolean depfilled;
	public Boolean editable;
	public JLabel namelabel = new JLabel();
	public JLabel compannlabel = new JLabel();
	public JLabel singularannlabel = new JLabel();
	public JLabel humdeflabel = new JLabel();
	public JLabel deplabel = new JLabel();
	public JPanel indicatorspanel = new JPanel();
	public JPanel indicatorssuperpanel = new JPanel();
	public PropertyMarker propoflabel = new PropertyMarker(Color.white, null);
	public int maxHeight = 35;
	public int ipph = 18;
	
	public AnnotationObjectButton(AnnotatorTab ann, SemSimComponent ssc, boolean compannfilled, String companntext, 
			boolean noncompannfilled, boolean humdeffilled, boolean depfilled, boolean editable) {
		this.annotater = ann;
		this.ssc = ssc;
		this.setLayout(new BorderLayout());
		this.noncompannfilled = noncompannfilled;
		this.companntext = companntext;
		this.compannfilled = compannfilled;
		this.humdeffilled = humdeffilled;
		this.depfilled = depfilled;
		this.editable = editable;
		this.setFocusable(true);
		this.addKeyListener(ann);
		this.setMaximumSize(new Dimension(999999, maxHeight));
		
		((BorderLayout)this.getLayout()).setVgap(0);
		((BorderLayout)this.getLayout()).setHgap(0);

		namelabel.setFont(SemGenFont.defaultPlain());
		namelabel.setOpaque(false);
		namelabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 10));
		namelabel.setBackground(new Color(0,0,0,0));
		namelabel.addMouseListener(this);
		
		setIdentifyingData(ssc.getName());
		
		indicatorspanel.setPreferredSize(new Dimension(50, ipph));
		indicatorspanel.setLayout(new BoxLayout(indicatorspanel, BoxLayout.X_AXIS));
		indicatorspanel.setAlignmentY(TOP_ALIGNMENT);
		indicatorspanel.setOpaque(false);

		compannlabel.setText("_");
		compannlabel.setName("C");
		compannlabel.setToolTipText("Indicates status of codeword's composite annotation");
		compannlabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		compannlabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		compannlabel.setFont(SemGenFont.Plain("Serif", -3));

		singularannlabel.setText("_");
		singularannlabel.setName("S");
		singularannlabel.setToolTipText("Click to set singular reference annotation");
		singularannlabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		singularannlabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		compannlabel.setFont(SemGenFont.Plain("Serif", -3));

		
		humdeflabel.setText("_");
		humdeflabel.setName("F");
		humdeflabel.setToolTipText("Click to set free-text description");
		humdeflabel.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
		humdeflabel.setAlignmentY(JComponent.CENTER_ALIGNMENT);
		compannlabel.setFont(SemGenFont.Plain("Serif", -3));

		deplabel.setText("_");
		deplabel.setName("D");
		
		if(editable){
			compannlabel.addMouseListener(this);
			singularannlabel.addMouseListener(this);
			humdeflabel.addMouseListener(this);
			deplabel.addMouseListener(this);
		}
		else namelabel.setForeground(Color.gray);

		if (compannfilled) {
			annotationAdded(compannlabel, true);
			compannlabel.setText(companntext);
		} 
		else {annotationNotAdded(compannlabel);}
		
		if (noncompannfilled) {annotationAdded(singularannlabel, false);} 
		else {annotationNotAdded(singularannlabel);}
		
		if (humdeffilled) {annotationAdded(humdeflabel, false);} 
		else {annotationNotAdded(humdeflabel);}
		
		if (depfilled) {annotationAdded(deplabel, false);}
		else {annotationNotAdded(deplabel);}
		
		indicatorspanel.add(compannlabel);
		indicatorspanel.add(singularannlabel);
		indicatorspanel.add(humdeflabel);
		indicatorspanel.setOpaque(false);
		
		indicatorssuperpanel.setOpaque(false);
		indicatorssuperpanel.setLayout(new BorderLayout());
		indicatorssuperpanel.add(Box.createGlue(), BorderLayout.WEST);
		indicatorssuperpanel.add(indicatorspanel, BorderLayout.CENTER);
		propoflabel.setVisible(false);
		indicatorssuperpanel.add(propoflabel, BorderLayout.EAST);

		add(Box.createGlue(), BorderLayout.EAST);
		add(namelabel, BorderLayout.CENTER);
		add(indicatorssuperpanel, BorderLayout.WEST);

		setBackground(Color.white);
		setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
		setOpaque(true);
		setForeground(Color.black);
		setVisible(true);
	}
	
	public void setIdentifyingData(String name){
		this.setName(name);
		namelabel.setText(name);
	}
	
	public boolean refreshAllCodes(){
		refreshFreeTextCode();
		refreshSingularAnnotationCode();
		annotater.updateTreeNode();
		return true;
	}
		
	public void refreshSingularAnnotationCode(){
		if(((Annotatable)ssc).hasRefersToAnnotation()){
			annotationAdded(singularannlabel, false);
		}
		else annotationNotAdded(singularannlabel);
		
	}
	
	public void refreshFreeTextCode(){
		if(ssc.getDescription()==null) annotationNotAdded(humdeflabel);
		else annotationAdded(humdeflabel, false);
	}

	public void annotationAdded(JLabel label, Boolean iscompann) {
		label.setFont(SemGenFont.Bold("Serif", -2));
		if(editable) label.setForeground(Color.blue);
		else label.setForeground(Color.gray);
		validate();
		repaint();
		if (!iscompann) {
			label.setText(label.getName());
		}
	}

	public void annotationNotAdded(JLabel label) {
		label.setFont(SemGenFont.Plain("Serif", -2));
		label.setForeground(Color.gray);
		label.setText("_");
		repaint();
		validate();
	}
	
	public void mouseEntered(MouseEvent e) {
		if (e.getComponent() instanceof JLabel && e.getComponent()!=compannlabel) {
			JLabel label = (JLabel) e.getComponent();
			if(label!=propoflabel){
				label.setOpaque(true);
				label.setBackground(new Color(255,231,186));
				label.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
		}
	}

	public void mouseExited(MouseEvent e) {
		if (e.getComponent() instanceof JLabel) {
			JLabel label = (JLabel) e.getComponent();
			if(label!=propoflabel){
				label.setOpaque(false);
				label.setBackground(null);
				label.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	public void mousePressed(MouseEvent e) {
		JLabel label = null;
		if(e.getComponent() instanceof JLabel || e.getComponent()==namelabel){
			if (e.getComponent() instanceof JLabel) {
				label = (JLabel) e.getComponent();
			}
			requestFocusInWindow();
			annotater.changeButtonFocus(annotater.focusbutton, this, label);
			annotater.focusbutton = this;
		}
	}
	
	public void mouseClicked(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

}
