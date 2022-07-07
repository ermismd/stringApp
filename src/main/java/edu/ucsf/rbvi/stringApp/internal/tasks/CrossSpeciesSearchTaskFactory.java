package edu.ucsf.rbvi.stringApp.internal.tasks;

import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.STRING_COLORS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.CROSS_SPECIES_LAYERS;
import static edu.ucsf.rbvi.stringApp.internal.utils.IconUtils.getIconFont;

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.Logger;

import org.cytoscape.application.CyUserLog;
import org.cytoscape.application.swing.search.AbstractNetworkSearchTaskFactory;
import org.cytoscape.application.swing.search.NetworkSearchTaskFactory;
import org.cytoscape.util.swing.LookAndFeelUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.TaskObserver;

import edu.ucsf.rbvi.stringApp.internal.model.Databases;
import edu.ucsf.rbvi.stringApp.internal.model.Species;
import edu.ucsf.rbvi.stringApp.internal.model.StringManager;
import edu.ucsf.rbvi.stringApp.internal.model.StringNetwork;
import edu.ucsf.rbvi.stringApp.internal.tasks.GetAnnotationsTask;
import edu.ucsf.rbvi.stringApp.internal.tasks.ImportNetworkTaskFactory;
import edu.ucsf.rbvi.stringApp.internal.ui.EasyGBC;
import edu.ucsf.rbvi.stringApp.internal.ui.JComboBoxDecorator;
import edu.ucsf.rbvi.stringApp.internal.ui.PubMedQueryPanel;
import edu.ucsf.rbvi.stringApp.internal.ui.SearchOptionsPanel;
import edu.ucsf.rbvi.stringApp.internal.utils.TextIcon;

public class CrossSpeciesSearchTaskFactory extends AbstractNetworkSearchTaskFactory {
	StringManager manager;
	static String CROSS_SPECIES_ID = "edu.ucsf.rbvi.x-species";
	static String CROSS_SPECIES_URL = "http://string-db.org";
	static String CROSS_SPECIES_NAME = "STRING Cross Species query";
	static String CROSS_SPECIES_DESC = "Search STRING for protein-protein interactions across species";
	static String CROSS_SPECIES_DESC_LONG =  "<html>The PubMed query retrieves a STRING network pertaining to any topic of interest <br />"
											+ "based on text mining of PubMed abstracts. STRING is a database of known and <br />"
											+ "predicted protein interactions for thousands of organisms, which are integrated <br />"
											+ "from several sources, scored, and transferred across orthologs. The network <br />"
											+ "includes both physical interactions and functional associations.</html>";

	private StringNetwork stringNetwork = null;
	private SearchOptionsPanel optionsPanel = null;
	private MySearchComponent queryComponent = null;
	private final Logger logger = Logger.getLogger(CyUserLog.NAME);
	private Font smallFont;
	private JButton sp1Button;
	private JButton sp2Button;
	private JPanel speciesPanel1;
	private JPanel speciesPanel2;
	private JFrame speciesFrame1;
	private JFrame speciesFrame2;

	private static final Icon icon = new TextIcon(CROSS_SPECIES_LAYERS, getIconFont(32.0f), STRING_COLORS, 36, 36);

	private static URL stringURL() {
		try {
			return new URL(CROSS_SPECIES_URL);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public CrossSpeciesSearchTaskFactory(StringManager manager) {
		super(CROSS_SPECIES_ID, CROSS_SPECIES_NAME, CROSS_SPECIES_DESC, icon, CrossSpeciesSearchTaskFactory.stringURL());
		this.manager = manager;
	}

	public boolean isReady() { 
		System.out.println("isReady");
		if (manager.haveURIs() && (queryComponent.getSpecies1() != null) && (queryComponent.getSpecies2() != null)) {
			System.out.println("true");
			return true; 
		}
		System.out.println("false");
		return false;
	}

	public TaskIterator createTaskIterator() {
		final Species species1 = (Species)queryComponent.getSpecies1();
		final Species species2 = (Species)queryComponent.getSpecies2();

		return new TaskIterator(new AbstractTask() {
			@Override
			public void run(TaskMonitor m) {
			}
		});

	}

	@Override
	public String getName() { return CROSS_SPECIES_NAME; }

	@Override
	public String getId() { return CROSS_SPECIES_ID; }

	@Override
	public String getDescription() {
		return CROSS_SPECIES_DESC_LONG;
	}

	@Override
	public Icon getIcon() {
		return icon;
	}

	@Override
	public URL getWebsite() { 
		return CrossSpeciesSearchTaskFactory.stringURL();
	}

	// Create a JPanel that provides the species, confidence interval, and number of interactions
	// NOTE: we need to use reasonable defaults since it's likely the user won't actually change it...
	@Override
	public JComponent getOptionsComponent() {
		optionsPanel = new SearchOptionsPanel(manager, true, false);
		return optionsPanel;
	}

	@Override
	public JComponent getQueryComponent() {
		if (queryComponent == null) {
			queryComponent = new MySearchComponent();
		}
		return queryComponent;
	}


	// We want a private class so we can add our helper text
	private class MySearchComponent extends JPanel {
		Color msgColor;
		final int vgap = 1;
		final int hgap = 5;
		private boolean haveFocus = false;
		JComboBox<Species> species1;
		JComboBox<String> species2;
		DefaultComboBoxModel model1;
		DefaultComboBoxModel model2;

		public MySearchComponent() {
			super();
			init();
		}

		Species getSpecies1() {
			return (Species)species1.getSelectedItem();
		}

		Species getSpecies2() {
			return Species.getSpecies((String)species2.getSelectedItem());
		}

		private void init() {
			msgColor = UIManager.getColor("Label.disabledForeground");
			setBackground(UIManager.getColor("TextField.background"));
			setLayout(new GridBagLayout());
			// setMinimumSize(getPreferredSize());
			// setBorder(BorderFactory.createEmptyBorder(vgap, hgap, vgap, hgap));
			smallFont = getFont().deriveFont(LookAndFeelUtil.getSmallFontSize());

			speciesPanel2 = createSpeciesPartnerComboBox(Species.getSpeciesPartners(Species.getHumanSpecies().toString()));

			EasyGBC c = new EasyGBC();

			sp1Button = new JButton();
			sp1Button.setFont(smallFont);
			add(sp1Button, c.insets(0,0,0,0));
			sp1Button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					speciesFrame1.validate();
					speciesFrame1.setVisible(true);
					speciesFrame1.setLocationRelativeTo(sp1Button);
				}
			});

			JLabel cross = new JLabel("X");
			cross.setFont(smallFont);
			add(cross, c.right().insets(0,10,0,0));

			sp2Button = new JButton("Species 2");
			sp2Button.setFont(smallFont);
			add(sp2Button, c.right().insets(0,10,0,0));
			sp2Button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					speciesFrame2.validate();
					speciesFrame2.setVisible(true);
					speciesFrame2.setLocationRelativeTo(sp2Button);
				}
			});


			// Get the current default species
			{
				speciesFrame1 = new JFrame();
				speciesFrame1.setPreferredSize(new Dimension(400,80));
				speciesPanel1 = createSpeciesComboBox();
				String sp1 = getSpecies1().toString();
				sp1 = sp1.substring(0, Math.min(sp1.length(), 20));
				sp1Button.setText(sp1);
				speciesFrame1.setTitle("First Species");
				speciesFrame1.getContentPane().add(speciesPanel1);
				speciesFrame1.setVisible(false);
				speciesFrame1.pack();
			}
			
			{
				speciesFrame2 = new JFrame();
				speciesFrame2.setPreferredSize(new Dimension(400,80));
				speciesPanel2 = createSpeciesPartnerComboBox(Species.getSpeciesPartners(Species.getHumanSpecies().toString()));
				speciesFrame2.setTitle("Second Species");
				speciesFrame2.getContentPane().add(speciesPanel2);
				speciesFrame2.setVisible(false);
				speciesFrame2.pack();
			}

			return;
		}

		JPanel createSpeciesComboBox() {
			List<Species> speciesList = Species.getModelSpecies();
			JPanel speciesPanel = new JPanel(new GridBagLayout());
			speciesPanel.setPreferredSize(new Dimension(200, 40));
			EasyGBC c = new EasyGBC();
			JLabel speciesLabel = new JLabel("Species 1:");
			speciesLabel.setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			c.noExpand().insets(0,0,0,0);
			speciesPanel.add(speciesLabel, c);
			species1 = new JComboBox<Species>(speciesList.toArray(new Species[1]));
			species1.setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			model1 = (DefaultComboBoxModel)species1.getModel();
	
			Species defaultSpecies = Species.getHumanSpecies();
			species1.setSelectedItem(defaultSpecies);
	
			JComboBoxDecorator.decorate(species1, true, true); 
			c.right().expandHoriz().insets(0,0,0,0);
			speciesPanel.add(species1, c);
	
			species1.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					System.out.println("Action performed");
					DefaultComboBoxModel<String> model2 = (DefaultComboBoxModel)species2.getModel();
					model2.removeAllElements();
					List<String> crossList = Species.getSpeciesPartners(species1.getSelectedItem().toString());
					Collections.sort(crossList);
					model2.addAll(crossList);
					speciesFrame1.setVisible(false);

					Species sp1 = (Species)species1.getSelectedItem();
					if (sp1 == null || sp1.toString() == "")
						sp1Button.setText("Species 1");
					else {
						String sp1str = sp1.toString();
						sp1str = sp1str.substring(0, Math.min(sp1str.length(), 20));
						sp1Button.setText(sp1str);
					}
					fireQueryChanged();

				}
			});
			return speciesPanel;
		}

		JPanel createSpeciesPartnerComboBox(List<String> speciesList) {
			JPanel speciesPanel = new JPanel(new GridBagLayout());
			EasyGBC c = new EasyGBC();
			JLabel speciesLabel = new JLabel("Species 2:");
			speciesLabel.setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			Collections.sort(speciesList);
			c.noExpand().insets(0,0,0,0);
			speciesPanel.add(speciesLabel, c);
			species2 = new JComboBox<String>(speciesList.toArray(new String[1]));
			species2.setFont(getFont().deriveFont(LookAndFeelUtil.getSmallFontSize()));
			model2 = (DefaultComboBoxModel)species2.getModel();
	
			if (speciesList.contains("Plasmodium falciparum"))
				species2.setSelectedItem("Plasmodium falciparum");
	
			c.right().expandHoriz().insets(0,0,0,0);
			speciesPanel.add(species2, c);

			species2.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent ae) {
					speciesFrame2.setVisible(false);
					String sp2 = (String)species2.getSelectedItem();
					if (sp2 == null || sp2.toString() == "")
						sp2Button.setText("Species 2");
					else {
						sp2 = sp2.substring(0, Math.min(sp2.length(), 20));
						sp2Button.setText(sp2);
					}
					fireQueryChanged();
				}
			});
			return speciesPanel;
		}

		private void fireQueryChanged() {
			MySearchComponent.this.firePropertyChange(NetworkSearchTaskFactory.QUERY_PROPERTY, null, null);
		}
	}

	@Override
	public TaskObserver getTaskObserver() { return null; }

}
