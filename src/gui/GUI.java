package gui;

import java.awt.EventQueue;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Color;

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSeparator;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.Box;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.ListSelectionModel;
import javax.swing.JScrollPane;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import gui.WmFileFilter.FilterType;
import pre_posts.Watermark;
import pre_posts.WatermarkException;
import processing.ApplyWatermark;
import processing.DoubleArray;
import processing.KeySettings;
import processing.ReadWatermark;

import javax.swing.JTextPane;

public class GUI {

	private static int fontSize = 14;
	
	private JFrame frmSpectromark;
	private JTextField textField_m_Amp;
	private JTable table_m_WavIn;
	private WmFileFilter fileFilter = new WmFileFilter();
	private JTextField txt_m_OutPathState;
	private JTextField txt_m_WmImgState;
	private JTextField txt_m_KeyState;
	private JSlider slider_m_Amp = new JSlider();
	private WavTaskTableModel tmodel_m_WavIn;
	private JLabel lbl_m_WmImgPrvw;
	private JSpinner spinner_m_KeyOffset;
	private JSpinner spinner_m_KeyWidth;
	private JSpinner spinner_m_KeyHeight;
	private JCheckBox chckbx_m_Smooth;
	private JCheckBox chckbx_m_Robust;
	private JSpinner spinner_m_FoldNum;
	private DefaultBoundedRangeModel brModel_m;
	private JProgressBar progressBar_m;
	private JButton btn_m_Abort;
	private JButton btn_m_ApplyWm;
	private JButton btn_m_SaveKey;
	
	private DefaultBoundedRangeModel brModel_r;
	private JProgressBar progressBar_r;
	private JButton btn_r_ReadWm;
	private JTextField txt_r_message;
	private JTextPane txt_r_Disp_SavePath;
	private JButton btn_r_Refresh;
	
	private File wmImg_m = null;
	private BufferedImage wmBuf_m = null;
	private ArrayList<WavTask> wavInList = new ArrayList<WavTask>();
	private File wavIn_m = null;//the variable that is passed to ApplyWatermark(...)
	private File wavOut_m = null;//the variable that is passed to ApplyWatermark(...)
	private File keyFile_m = null;//the default keyFile (used when wavTask.genNewKey is false)
	private KeySettings keySettings_m = new KeySettings(-0.75, 256, 256, false, false);
	private int keyFoldNumber_m = 2;
	private double amp;//the amplitude/energy of the watermark
	private Watermark watermarkApply = new Watermark(false);
	private final File defaultResourcePath = new File(".\\res\\anyFile");
	private boolean listenerActive_m = true;
	private boolean isReadiedToMark = false;
	private final SignalFlag encode_flag = new SignalFlag(false);
	
	private File wavIn_r = null;//the variable that is passed to ReadWatermark(...)
	private File keyFile_r = null;//the variable that is passed to ReadWatermark(...)
	private String imageFilename = null;//the image filename template for saving the decoded watermarks
	private Watermark watermarkRead = new Watermark(true);
	private VisWmBlur blur = new VisWmBlur(0.65, 1.5);
	private VisWmColor color = new VisWmColor(0.0, 0.5, -0.2, -0.1);
	private boolean isReadiedToRead = false;
	private final SignalFlag decode_flag = new SignalFlag(false);
	private DoubleArray array_r_res_mask = new DoubleArray();
	private ImageIcon icon_r_res1 = new ImageIcon();
	private ImageIcon icon_r_res2 = new ImageIcon();
	
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					//UIManager.setLookAndFeel("Metal");
					GUI window = new GUI();
					window.frmSpectromark.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	public static BufferedImage toBufferedImage(Image img) {
		// https://www.imooc.com/wenda/detail/600880
	    if (img instanceof BufferedImage) {
	        return (BufferedImage) img;
	    }

	    // Create a buffered image with transparency
	    BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

	    // Draw the image on to the buffered image
	    Graphics2D bGr = bimage.createGraphics();
	    bGr.drawImage(img, 0, 0, null);
	    bGr.dispose();
	    
	    // Return the buffered image
	    return bimage;
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		
		Color buttonBgd = new Color(210,225,240);
		
		frmSpectromark = new JFrame();
		
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		frmSpectromark.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		frmSpectromark.setMinimumSize(new Dimension(1200, 600));
		
		JPanel panel_marker = new JPanel();
		tabbedPane.addTab("Marker", null, panel_marker, null);
		GridBagLayout gbl_panel_marker = new GridBagLayout();
		gbl_panel_marker.columnWidths = new int[]{0, 0};
		gbl_panel_marker.rowHeights = new int[]{150, 0, 0};
		gbl_panel_marker.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_marker.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		panel_marker.setLayout(gbl_panel_marker);
		
		JLabel lbl_m_Title = new JLabel("");
		lbl_m_Title.setIcon(new ImageIcon("./res/LOGO_Spectromark_h120px.png"));
		GridBagConstraints gbc_lbl_m_Title = new GridBagConstraints();
		gbc_lbl_m_Title.insets = new Insets(0, 0, 5, 0);
		gbc_lbl_m_Title.gridx = 0;
		gbc_lbl_m_Title.gridy = 0;
		panel_marker.add(lbl_m_Title, gbc_lbl_m_Title);
		
		JSplitPane splitPane_m_LR = new JSplitPane();
		splitPane_m_LR.setDividerSize(5);
		splitPane_m_LR.setContinuousLayout(true);
		GridBagConstraints gbc_splitPane_m_LR = new GridBagConstraints();
		gbc_splitPane_m_LR.fill = GridBagConstraints.BOTH;
		gbc_splitPane_m_LR.gridx = 0;
		gbc_splitPane_m_LR.gridy = 1;
		panel_marker.add(splitPane_m_LR, gbc_splitPane_m_LR);
		
		JPanel panel_m_L = new JPanel();
		splitPane_m_LR.setLeftComponent(panel_m_L);
		GridBagLayout gbl_panel_m_L = new GridBagLayout();
		gbl_panel_m_L.columnWidths = new int[]{20, 60, 20, 30, 30, 10};
		gbl_panel_m_L.rowHeights = new int[]{50, 150, 30, 30, 60, 15, 15};
		gbl_panel_m_L.columnWeights = new double[]{0.0, 1.0, 1.0, 0.0, 0.0, 0.0};
		gbl_panel_m_L.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		panel_m_L.setLayout(gbl_panel_m_L);
		
		JLabel lbl_m_WmImg_Title = new JLabel("Watermark Image");
		lbl_m_WmImg_Title.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_WmImg_Title.setToolTipText("The watermark identifier");
		GridBagConstraints gbc_lbl_m_WmImg_Title = new GridBagConstraints();
		gbc_lbl_m_WmImg_Title.anchor = GridBagConstraints.WEST;
		gbc_lbl_m_WmImg_Title.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_WmImg_Title.gridx = 1;
		gbc_lbl_m_WmImg_Title.gridy = 0;
		panel_m_L.add(lbl_m_WmImg_Title, gbc_lbl_m_WmImg_Title);
		
		JButton btn_m_WmImg = new JButton("Select...");
		btn_m_WmImg.setBackground(buttonBgd);
		btn_m_WmImg.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_WmImg.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				File wmImg_backup = wmImg_m;
				try {
					wmImg_m = browseFile("JRE supported Image Files", ImageIO.getReaderFileSuffixes(), 
		            		wmImg_m, false);
		        } catch (Exception ex) {
		        	wmImg_m = browseFile("JRE supported Image Files", ImageIO.getReaderFileSuffixes(), 
		        			defaultResourcePath, false);
		            //ex.printStackTrace();
		        }
		        if(wmImg_m != null) {
					try {
						wmBuf_m = ImageIO.read(wmImg_m);
						watermarkApply.loadImg(wmImg_m);
						refreshWmToKeySettings();
						displayKeySettings();
						if(wmBuf_m != null) { 
							lbl_m_WmImgPrvw.setIcon(fitImageonLabel(wmBuf_m, lbl_m_WmImgPrvw));
							txt_m_WmImgState.setText(wmImg_m.getName());
						}
						else txt_m_WmImgState.setText("Invalid image file");
					} catch (IOException e) {
						JOptionPane.showMessageDialog(null, "Error reading watermark image file!\n"
								+ "Please choose another.");
						wmImg_m = wmImg_backup;
						e.printStackTrace();
					}
				}
		        refreshIsReadiedToMark();
			}
		});
		
		Component horizontalStrut_m_wmImg_Sel = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_m_wmImg_Sel = new GridBagConstraints();
		gbc_horizontalStrut_m_wmImg_Sel.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_m_wmImg_Sel.gridx = 2;
		gbc_horizontalStrut_m_wmImg_Sel.gridy = 0;
		panel_m_L.add(horizontalStrut_m_wmImg_Sel, gbc_horizontalStrut_m_wmImg_Sel);
		btn_m_WmImg.setToolTipText("Select a watermark image.");
		GridBagConstraints gbc_btn_m_WmImg = new GridBagConstraints();
		gbc_btn_m_WmImg.anchor = GridBagConstraints.EAST;
		gbc_btn_m_WmImg.gridwidth = 2;
		gbc_btn_m_WmImg.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_WmImg.gridx = 3;
		gbc_btn_m_WmImg.gridy = 0;
		panel_m_L.add(btn_m_WmImg, gbc_btn_m_WmImg);
		
		lbl_m_WmImgPrvw = new JLabel("");
		lbl_m_WmImgPrvw.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_m_WmImgPrvw.setToolTipText("Drop the watermark image here");
		lbl_m_WmImgPrvw.setDropTarget(new DropTarget() {
		    /**
			 * 
			 */
			private static final long serialVersionUID = 234567L;

			public synchronized void drop(DropTargetDropEvent evt) {
		    	File wmImg_backup = wmImg_m;
		        try {
		            evt.acceptDrop(DnDConstants.ACTION_COPY);
		            List<File> droppedWmFiles = (List<File>)
		                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
		            fileFilter.setFilterType(FilterType.Image);
		            for (File file : droppedWmFiles) {
		                if(fileFilter.accept(file)) {
		                	wmImg_m = file;
		                	break;
		                }
		                else txt_m_WmImgState.setText("Image format not supported by JRE");
		            }
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		        if(wmImg_m != null) {
					try {
						wmBuf_m = ImageIO.read(wmImg_m);
						watermarkApply.loadImg(wmImg_m);
						refreshWmToKeySettings();
						displayKeySettings();
						if(wmBuf_m != null) { 
							lbl_m_WmImgPrvw.setIcon(fitImageonLabel(wmBuf_m, lbl_m_WmImgPrvw));
							txt_m_WmImgState.setText(wmImg_m.getName());
						}
						else txt_m_WmImgState.setText("Invalid image file");
					} catch (IOException e) {
						JOptionPane.showMessageDialog(null, "Error reading watermark image file!\n"
								+ "Please choose another.");
						wmImg_m = wmImg_backup;
						e.printStackTrace();
					}
				}
		        refreshIsReadiedToMark();
		    }
		});
		ComponentListener l_WmImgPrvw = new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent evt) {
			}
			@Override
			public void componentShown(ComponentEvent evt) {
			}
			@Override
			public void componentMoved(ComponentEvent evt) {
			}
			@Override
			public void componentResized(ComponentEvent evt) {
				if(wmBuf_m != null) lbl_m_WmImgPrvw.setIcon(fitImageonLabel(wmBuf_m, lbl_m_WmImgPrvw));
			}
		};
		lbl_m_WmImgPrvw.addComponentListener(l_WmImgPrvw);
		GridBagConstraints gbc_lbl_m_WmImgPrvw = new GridBagConstraints();
		gbc_lbl_m_WmImgPrvw.fill = GridBagConstraints.BOTH;
		gbc_lbl_m_WmImgPrvw.gridwidth = 4;
		gbc_lbl_m_WmImgPrvw.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_WmImgPrvw.gridx = 1;
		gbc_lbl_m_WmImgPrvw.gridy = 1;
		panel_m_L.add(lbl_m_WmImgPrvw, gbc_lbl_m_WmImgPrvw);
		
		txt_m_WmImgState = new JTextField();
		txt_m_WmImgState.setHorizontalAlignment(SwingConstants.CENTER);
		txt_m_WmImgState.setBorder(null);
		txt_m_WmImgState.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize*5/6));
		txt_m_WmImgState.setText("Please select an image as watermark");
		txt_m_WmImgState.setEditable(false);
		txt_m_WmImgState.setBackground(UIManager.getColor("Panel.background"));
		GridBagConstraints gbc_txt_m_WmImgState = new GridBagConstraints();
		gbc_txt_m_WmImgState.gridwidth = 4;
		gbc_txt_m_WmImgState.insets = new Insets(0, 0, 5, 5);
		gbc_txt_m_WmImgState.fill = GridBagConstraints.HORIZONTAL;
		gbc_txt_m_WmImgState.gridx = 1;
		gbc_txt_m_WmImgState.gridy = 2;
		panel_m_L.add(txt_m_WmImgState, gbc_txt_m_WmImgState);
		txt_m_WmImgState.setColumns(10);
		
		JLabel lbl_m_Amp = new JLabel("Watermark Amplitude");
		lbl_m_Amp.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_Amp.setToolTipText("<html><p width=\"400\">Spectromark applies audio watermarks "
				+ "by modifying the amplitude of certain frequencies in a time frame through an "
				+ "STFT (Short Time Fourier Tranformation). This \"Amplitude\" means the amplitude "
				+ "of the applied watermark, or how much the frequencies are modified. With the "
				+ "watermark amplitude being 0, the audio remains unchanged, with 1, the frequencies "
				+ "are filtered out (completely modified).<br>Recommended value: 0.3</p></html>");
		GridBagConstraints gbc_lbl_m_Amp = new GridBagConstraints();
		gbc_lbl_m_Amp.gridwidth = 3;
		gbc_lbl_m_Amp.anchor = GridBagConstraints.WEST;
		gbc_lbl_m_Amp.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_Amp.gridx = 1;
		gbc_lbl_m_Amp.gridy = 3;
		panel_m_L.add(lbl_m_Amp, gbc_lbl_m_Amp);
		
		textField_m_Amp = new JTextField();
		KeyListener l_m_AmpTxt = new KeyListener() {
			private void update() {
				double amp_temp = amp;
		    	try {
		    		amp_temp = Double.parseDouble(textField_m_Amp.getText());
		    	} catch (NumberFormatException ne) {
		    		textField_m_Amp.setText(Double.toString(amp));
		    	}
		        if (amp_temp < 0.0){
		            amp_temp = 0.0;
		            textField_m_Amp.setText("0.0");
		            
		        }    
		        if (amp_temp > 1.0){
		            amp_temp = 1.0;
		            textField_m_Amp.setText("1.0");
		        }
		        amp = amp_temp;
		        slider_m_Amp.setValue((int)(amp*100));
			}
			@Override
			public void keyPressed(KeyEvent e) {
				update();
			}
			@Override
			public void keyReleased(KeyEvent e) {
			}
			@Override
			public void keyTyped(KeyEvent e) {
			}
		};
		textField_m_Amp.addKeyListener(l_m_AmpTxt);
		textField_m_Amp.setHorizontalAlignment(SwingConstants.CENTER);
		textField_m_Amp.setText("0.30");
		textField_m_Amp.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_textField_m_Amp = new GridBagConstraints();
		gbc_textField_m_Amp.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField_m_Amp.anchor = GridBagConstraints.EAST;
		gbc_textField_m_Amp.insets = new Insets(0, 0, 5, 5);
		gbc_textField_m_Amp.gridx = 4;
		gbc_textField_m_Amp.gridy = 3;
		panel_m_L.add(textField_m_Amp, gbc_textField_m_Amp);
		textField_m_Amp.setColumns(4);
		
		ChangeListener l_m_AmpSld = new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				amp = (double)slider_m_Amp.getValue() / 100.0;
				textField_m_Amp.setText(String.valueOf(amp));
			}
		};
		slider_m_Amp.addChangeListener(l_m_AmpSld);
		slider_m_Amp.setValue(30);
		slider_m_Amp.setMinorTickSpacing(1);
		slider_m_Amp.setMajorTickSpacing(10);
		slider_m_Amp.setSnapToTicks(true);
		slider_m_Amp.setPaintTicks(true);
		GridBagConstraints gbc_slider_m_Amp = new GridBagConstraints();
		gbc_slider_m_Amp.insets = new Insets(0, 0, 5, 5);
		gbc_slider_m_Amp.fill = GridBagConstraints.BOTH;
		gbc_slider_m_Amp.gridwidth = 4;
		gbc_slider_m_Amp.gridx = 1;
		gbc_slider_m_Amp.gridy = 4;
		panel_m_L.add(slider_m_Amp, gbc_slider_m_Amp);
		
		JLabel lbl_m_AmpLgd = new JLabel("");
		ComponentListener l_m_AmpLgd = new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent evt) {
			}
			@Override
			public void componentShown(ComponentEvent evt) {
			}
			@Override
			public void componentMoved(ComponentEvent evt) {
			}
			@Override
			public void componentResized(ComponentEvent evt) {
				try {
					BufferedImage img = ImageIO.read(new File("./res/AmpLegend.png"));
					lbl_m_AmpLgd.setIcon(new ImageIcon(img.getScaledInstance(lbl_m_AmpLgd.getWidth(), lbl_m_AmpLgd.getHeight(), Image.SCALE_SMOOTH)));
				} catch (IOException e1) {
					System.out.println("Failed resizing the watermark image.");
					e1.printStackTrace();
				}
			}
		};
		lbl_m_WmImgPrvw.addComponentListener(l_m_AmpLgd);
		GridBagConstraints gbc_lbl_m_AmpLgd = new GridBagConstraints();
		gbc_lbl_m_AmpLgd.fill = GridBagConstraints.BOTH;
		gbc_lbl_m_AmpLgd.gridwidth = 4;
		gbc_lbl_m_AmpLgd.insets = new Insets(0, 0, 0, 5);
		gbc_lbl_m_AmpLgd.gridx = 1;
		gbc_lbl_m_AmpLgd.gridy = 5;
		panel_m_L.add(lbl_m_AmpLgd, gbc_lbl_m_AmpLgd);
		
		JPanel panel_m_R = new JPanel();
		splitPane_m_LR.setRightComponent(panel_m_R);
		GridBagLayout gbl_panel_m_R = new GridBagLayout();
		gbl_panel_m_R.columnWidths = new int[]{20, 80, 20, 80, 80, 80, 60, 80, 20};
		gbl_panel_m_R.rowHeights = new int[]{50, 120, 60, 40};
		gbl_panel_m_R.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0};
		gbl_panel_m_R.rowWeights = new double[]{0.0, 1.0, 0.0, 0.0};
		panel_m_R.setLayout(gbl_panel_m_R);
		
		JButton btn_m_SelWavIn = new JButton("Select Files...");
		btn_m_SelWavIn.setBackground(buttonBgd);
		btn_m_SelWavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_SelWavIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int numTskBeforeAdd = wavInList.size();
				File[] selectedWavFiles = new File[1];
				try {
					selectedWavFiles[0] = wavInList.get(0).getFile();
				} catch (Exception e1) {
					//Empty wavInList, use default path
					selectedWavFiles[0] = defaultResourcePath;
					//e1.printStackTrace();
				}
				selectedWavFiles = browseFiles("", new String[] {"wav"}, selectedWavFiles[0]);
				if(selectedWavFiles == null) return;
				if(selectedWavFiles[0] == null) return;
				boolean repeatedFile = false;
				WavTask tsk;
				for (File file : selectedWavFiles) {
	            	repeatedFile = false;
	            	
	            	tsk = new WavTask(file, true);
	            	
	                if(fileFilter.accept(file)) {
	                	for(WavTask existingTask : wavInList) {
	                		if(existingTask.isSameFile(tsk)) {
	                			repeatedFile = true;
	                			break;
	                		}
	                	}
	                	if(repeatedFile) continue;
	                	else {
	                		wavInList.add(tsk);
	                		//http://www.codejava.net/java-se/swing/editable-jtable-example
	                		//https://docs.oracle.com/javase/tutorial/uiswing/components/table.html
	                	}
	                	txt_m_OutPathState.setText("Please import wav files or set output path manually  ");
	                }
	                else txt_m_OutPathState.setText("Unsupported format. Please drop *.wav files here  ");
	            }
				tmodel_m_WavIn.refreshTable(wavInList);
				int numTskAfterAdd = wavInList.size();
	            tmodel_m_WavIn.fireTableRowsInserted(numTskBeforeAdd, numTskAfterAdd-1);
	            refreshIsReadiedToMark();
	            if(numTskAfterAdd > 0) 
	            	wavIn_m = wavInList.get(0).getFile();
			}
		});
		
		JLabel lbl_m_WavIn = new JLabel("WAV Files to Mark");
		lbl_m_WavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_WavIn.setToolTipText("The *.wav files to be watermarked");
		GridBagConstraints gbc_lbl_m_WavIn = new GridBagConstraints();
		gbc_lbl_m_WavIn.anchor = GridBagConstraints.WEST;
		gbc_lbl_m_WavIn.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_WavIn.gridx = 1;
		gbc_lbl_m_WavIn.gridy = 0;
		panel_m_R.add(lbl_m_WavIn, gbc_lbl_m_WavIn);
		
		Component horizontalStrut_m_WavTitle_Sel = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_m_WavTitle_Sel = new GridBagConstraints();
		gbc_horizontalStrut_m_WavTitle_Sel.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_m_WavTitle_Sel.gridx = 2;
		gbc_horizontalStrut_m_WavTitle_Sel.gridy = 0;
		panel_m_R.add(horizontalStrut_m_WavTitle_Sel, gbc_horizontalStrut_m_WavTitle_Sel);
		btn_m_SelWavIn.setToolTipText("Add files to the List");
		GridBagConstraints gbc_btn_m_SelWavIn = new GridBagConstraints();
		gbc_btn_m_SelWavIn.fill = GridBagConstraints.HORIZONTAL;
		gbc_btn_m_SelWavIn.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_SelWavIn.gridx = 3;
		gbc_btn_m_SelWavIn.gridy = 0;
		panel_m_R.add(btn_m_SelWavIn, gbc_btn_m_SelWavIn);
		
		JButton btn_m_RmvWavIn = new JButton("Remove Unselected Files");
		btn_m_RmvWavIn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int numTskBeforeDel = wavInList.size();
				for(int i=wavInList.size()-1; i>=0; i--) {
					if(!wavInList.get(i).isSelected()) { wavInList.remove(i);}
				}
				tmodel_m_WavIn.refreshTable(wavInList);
				int numTskAfterDel = wavInList.size();
				if(numTskAfterDel > 0) {
	            	wavIn_m = wavInList.get(0).getFile();
					tmodel_m_WavIn.fireTableRowsUpdated(0, numTskAfterDel-1);
					tmodel_m_WavIn.fireTableRowsDeleted(numTskAfterDel, numTskBeforeDel-1);
					}
				refreshIsReadiedToMark();
			}
		});
		btn_m_RmvWavIn.setBackground(buttonBgd);
		btn_m_RmvWavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_RmvWavIn.setToolTipText("Remove from list. Files stay on disk.");
		GridBagConstraints gbc_btn_m_RmvWavIn = new GridBagConstraints();
		gbc_btn_m_RmvWavIn.fill = GridBagConstraints.HORIZONTAL;
		gbc_btn_m_RmvWavIn.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_RmvWavIn.gridx = 4;
		gbc_btn_m_RmvWavIn.gridy = 0;
		panel_m_R.add(btn_m_RmvWavIn, gbc_btn_m_RmvWavIn);
		
		txt_m_OutPathState = new JTextField();
		txt_m_OutPathState.setBorder(null);
		txt_m_OutPathState.setText("Please import wav files or set output path manually  ");
		txt_m_OutPathState.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize*5/6));
		txt_m_OutPathState.setEditable(false);
		txt_m_OutPathState.setHorizontalAlignment(SwingConstants.RIGHT);
		txt_m_OutPathState.setBackground(UIManager.getColor("Panel.background"));
		GridBagConstraints gbc_txt_m_OutPathState = new GridBagConstraints();
		gbc_txt_m_OutPathState.gridwidth = 2;
		gbc_txt_m_OutPathState.insets = new Insets(0, 0, 5, 5);
		gbc_txt_m_OutPathState.fill = GridBagConstraints.HORIZONTAL;
		gbc_txt_m_OutPathState.gridx = 5;
		gbc_txt_m_OutPathState.gridy = 0;
		panel_m_R.add(txt_m_OutPathState, gbc_txt_m_OutPathState);
		txt_m_OutPathState.setColumns(10);
		
		JButton btn_m_SelWavOutPath = new JButton("Set Output Path...");
		btn_m_SelWavOutPath.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File wavOut_new = wavOut_m;
				File defaultOut = defaultResourcePath;
				if(wavIn_m != null) defaultOut = wavIn_m;
				if(wavOut_m != null) {
					wavOut_new = browseFile("All Files", new String[] {}, wavOut_m, true);
				} else {
					wavOut_new = browseFile("All Files", new String[] {}, defaultOut, true);
				}
				if(wavOut_new != null) {
					if(wavOut_new.isDirectory()) {//not a file, but a directory
						wavOut_m = wavOut_new;
					} else wavOut_m = wavOut_new.getParentFile();//if wavOut_new is a file, take its parent path
					txt_m_OutPathState.setText("Output to: " + wavOut_m.getAbsolutePath());
				}
			}
		});
		btn_m_SelWavOutPath.setBackground(buttonBgd);
		btn_m_SelWavOutPath.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_SelWavOutPath.setToolTipText("Set where the marked audio files are stored.");
		GridBagConstraints gbc_btn_m_SelWavOutPath = new GridBagConstraints();
		gbc_btn_m_SelWavOutPath.fill = GridBagConstraints.HORIZONTAL;
		gbc_btn_m_SelWavOutPath.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_SelWavOutPath.gridx = 7;
		gbc_btn_m_SelWavOutPath.gridy = 0;
		panel_m_R.add(btn_m_SelWavOutPath, gbc_btn_m_SelWavOutPath);
		
		tmodel_m_WavIn = new WavTaskTableModel(wavInList);
		tmodel_m_WavIn.addTableModelListener(new TableModelListener() {
			public void tableChanged(TableModelEvent e) {
				refreshIsReadiedToMark();
			}
		});
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.gridwidth = 6;
		gbc_scrollPane.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 1;
		panel_m_R.add(scrollPane, gbc_scrollPane);
		table_m_WavIn = new JTable(tmodel_m_WavIn);
		scrollPane.setViewportView(table_m_WavIn);
		table_m_WavIn.getColumnModel().getColumn(0).setPreferredWidth(50);
		table_m_WavIn.getColumnModel().getColumn(1).setPreferredWidth(200);
		table_m_WavIn.getColumnModel().getColumn(2).setPreferredWidth(100);
		table_m_WavIn.getColumnModel().getColumn(3).setPreferredWidth(500);
		table_m_WavIn.setRowHeight(28);
		table_m_WavIn.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		table_m_WavIn.setFillsViewportHeight(true);
		table_m_WavIn.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_m_WavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		table_m_WavIn.setToolTipText("You may drop *.wav files here");
		table_m_WavIn.setDropTarget(new DropTarget() {
		    /**
			 * 
			 */
			private static final long serialVersionUID = -498806594748422708L;

			public synchronized void drop(DropTargetDropEvent evt) {
		    	WavTask tsk;
		    	boolean repeatedFile;
		        try {
		        	int numTskBeforeAdd = wavInList.size();
		            evt.acceptDrop(DnDConstants.ACTION_COPY);
		            List<File> droppedWavFiles = (List<File>)
		                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
		            fileFilter.setFilterType(FilterType.Wav);
		            for (File file : droppedWavFiles) {
		            	repeatedFile = false;
		            	
		            	tsk = new WavTask(file, true);
		            	
		                if(fileFilter.accept(file)) {
		                	for(WavTask existingTask : wavInList) {
		                		if(existingTask.isSameFile(tsk)) {
		                			repeatedFile = true;
		                			break;
		                		}
		                	}
		                	if(repeatedFile) continue;
		                	else {
		                		wavInList.add(tsk);
		                		//http://www.codejava.net/java-se/swing/editable-jtable-example
		                		//https://docs.oracle.com/javase/tutorial/uiswing/components/table.html
		                	}
		                	txt_m_OutPathState.setText("Please import wav files or set output path manually  ");
		                }
		                else txt_m_OutPathState.setText("Unsupported format. Please drop *.wav files here  ");
		            }
		            tmodel_m_WavIn.refreshTable(wavInList);
		            int numTskAfterAdd = wavInList.size();
		            tmodel_m_WavIn.fireTableRowsInserted(numTskBeforeAdd, numTskAfterAdd-1);
		            if(numTskAfterAdd > 0) 
		            	wavIn_m = wavInList.get(0).getFile();
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		        refreshIsReadiedToMark();
		    }
		});
//		table_m_WavIn.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
//			@Override
//			public void valueChanged(ListSelectionEvent e) {
//				if (!e.getValueIsAdjusting()) {
////					(undefined variable!) selRow = table_m_WavIn.getSelectedRow();
//		        }
//			}
//		});
		
		btn_m_ApplyWm = new JButton("Apply Watermark");
		btn_m_ApplyWm.setEnabled(isReadiedToMark);
		btn_m_ApplyWm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new Runnable() {
			        public void run() {
			        	if(!watermarkApply.isKeySaved()) {
			        		if(btn_m_SaveKey.getActionListeners() != null) {
			        			btn_m_SaveKey.getActionListeners()[0].actionPerformed(new ActionEvent(btn_m_SaveKey, 0, ""));
			        		}
			        	}
			        	ApplyWatermark applyWm;
			        	WavTask wavTask;
			        	encode_flag.setGoOn(true);
			        	btn_m_Abort.setEnabled(true);
			        	progressBar_m.setString(null);//Display the percentage
			        	if(wavOut_m == null) txt_m_OutPathState.setText("Outs are saved in the input path.");
			        	
						for(int taskIndex=0; taskIndex<wavInList.size(); taskIndex++) {
							wavTask = wavInList.get(taskIndex);
							if(!wavTask.isSelected()) continue;
							
							table_m_WavIn.changeSelection(taskIndex, 0, false, false);
							wavIn_m = wavTask.getFile();
							File wavOutThis = wavOut_m;
							if(wavOut_m == null) wavOutThis = wavIn_m;
							applyWm = new ApplyWatermark(wavIn_m, wavOutThis, watermarkApply);
							//Google: Java task / JProgressbar
							//https://docs.oracle.com/javase/9/docs/api/index.html?javafx/concurrent/Task.html
							//https://docs.oracle.com/javase/7/docs/api/javax/swing/JProgressBar.html
							//https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html
							applyWm.encode(amp, brModel_m, encode_flag);
							if(!encode_flag.isGoOn()) {
								progressBar_m.setString("Aborted");
								encode_flag.setGoOn(true);
								break;
							}
							
							wavTask.setSelected(false);
							wavInList.set(taskIndex, wavTask);
							tmodel_m_WavIn.fireTableRowsUpdated(taskIndex, taskIndex);
						}
						return;
			        }
			    });
				t.start();
			}
		});
		btn_m_ApplyWm.setBackground(buttonBgd);
		btn_m_ApplyWm.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_ApplyWm.setToolTipText("<html><p width=\"400\">Apply watermark to ticked (selected) files and save them "
				+ "to the specified path with an appended \"-marked\" at the end of the name of each file.</p></html>");
		GridBagConstraints gbc_btn_m_ApplyWm = new GridBagConstraints();
		gbc_btn_m_ApplyWm.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_ApplyWm.fill = GridBagConstraints.BOTH;
		gbc_btn_m_ApplyWm.gridx = 7;
		gbc_btn_m_ApplyWm.gridy = 1;
		panel_m_R.add(btn_m_ApplyWm, gbc_btn_m_ApplyWm);
		
		JButton btn_m_SelKey = new JButton("Select Key...");
		btn_m_SelKey.setDropTarget(new DropTarget() {
		    /**
			 * 
			 */
			private static final long serialVersionUID = -498806594748422708L;

			public synchronized void drop(DropTargetDropEvent evt) {
		        try {
		            evt.acceptDrop(DnDConstants.ACTION_COPY);
		            List<File> droppedKeyFiles = (List<File>)
		                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
		            fileFilter.setFilterType(FilterType.Key);
		            Watermark wm_backup = watermarkApply;
		            for (File file : droppedKeyFiles) {
		                if(fileFilter.accept(file)) {
	    					try {
	    						watermarkApply.loadKey(file);
	    						txt_m_KeyState.setText("Key loaded: " + file.getName());
	    						
	    						keyFile_m = file;
	    						refreshWmToKeySettings();
	    						displayKeySettings();
	    						tmodel_m_WavIn.fireTableRowsUpdated(0, wavInList.size()-1);
	    						break;
	    					} catch(WatermarkException eWm) {
	    						watermarkApply = wm_backup;
	    						txt_m_KeyState.setText("Invalid key! Current key unchanged.");
	    					}
		                }
		            }
		            
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		        refreshIsReadiedToMark();
		    }
		});
		btn_m_SelKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File keyFile_backup = keyFile_m;
				if(keyFile_m == null && wavOut_m == null) {
					keyFile_m = browseFile("Key file *.bin", new String[] {"bin"}, defaultResourcePath, false);
				} else if(keyFile_m == null){
					keyFile_m = browseFile("Key file *.bin", new String[] {"bin"}, wavOut_m, false);
				} else {
					keyFile_m = browseFile("Key file *.bin", new String[] {"bin"}, keyFile_m, false);
				}
				if(keyFile_m != null) {
					Watermark wm_backup = watermarkApply;
					try {
						watermarkApply.loadKey(keyFile_m);
						txt_m_KeyState.setText("Key loaded: " + keyFile_m.getName());
						
						refreshWmToKeySettings();
						displayKeySettings();
						refreshIsReadiedToMark();
						tmodel_m_WavIn.fireTableRowsUpdated(0, wavInList.size()-1);
					} catch(WatermarkException eWm) {
						keyFile_m = keyFile_backup;
						watermarkApply = wm_backup;
						txt_m_KeyState.setText("Invalid key! Current key unchanged.");
					}
				} else keyFile_m = keyFile_backup;
			}
		});
		btn_m_SelKey.setBackground(buttonBgd);
		btn_m_SelKey.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_SelKey.setToolTipText("Select an *.bin key file or drop it here.");
		GridBagConstraints gbc_btn_m_SelKey = new GridBagConstraints();
		gbc_btn_m_SelKey.fill = GridBagConstraints.BOTH;
		gbc_btn_m_SelKey.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_SelKey.gridx = 1;
		gbc_btn_m_SelKey.gridy = 2;
		panel_m_R.add(btn_m_SelKey, gbc_btn_m_SelKey);
		
		JPanel panel_m_KeyInfo = new JPanel();
		GridBagConstraints gbc_panel_m_KeyInfo = new GridBagConstraints();
		gbc_panel_m_KeyInfo.gridwidth = 4;
		gbc_panel_m_KeyInfo.insets = new Insets(0, 0, 5, 5);
		gbc_panel_m_KeyInfo.fill = GridBagConstraints.BOTH;
		gbc_panel_m_KeyInfo.gridx = 2;
		gbc_panel_m_KeyInfo.gridy = 2;
		panel_m_R.add(panel_m_KeyInfo, gbc_panel_m_KeyInfo);
		GridBagLayout gbl_panel_m_KeyInfo = new GridBagLayout();
		gbl_panel_m_KeyInfo.columnWidths = new int[]{60, 90, 5, 50, 50, 5, 50, 10, 50, 50, 5};
		gbl_panel_m_KeyInfo.rowHeights = new int[]{0, 0, 0};
		gbl_panel_m_KeyInfo.columnWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0};
		gbl_panel_m_KeyInfo.rowWeights = new double[]{1.0, 1.0, Double.MIN_VALUE};
		panel_m_KeyInfo.setLayout(gbl_panel_m_KeyInfo);
		
		JLabel lbl_m_Offset = new JLabel("Offset");
		lbl_m_Offset.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_Offset.setToolTipText("<html><p width=\"400\">Watermark parameter. Describes where in the spectrum is the watermark placed. Ranging from -100 to 100. With -100 the watermark is placed at the low end of the spectrum and the bass can be influenced to an audible extend. With 100 only the high end of the spectrum is influenced. Due to the fact that lossy compressed audio often lose their high frequencies, the watermark can be damaged. The recommended value is -75.</p></html>");
		GridBagConstraints gbc_lbl_m_Offset = new GridBagConstraints();
		gbc_lbl_m_Offset.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_Offset.gridx = 0;
		gbc_lbl_m_Offset.gridy = 0;
		panel_m_KeyInfo.add(lbl_m_Offset, gbc_lbl_m_Offset);
		
		spinner_m_KeyOffset = new JSpinner();
		spinner_m_KeyOffset.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		spinner_m_KeyOffset.setModel(new SpinnerNumberModel(-75, -100, 100, 5));
		spinner_m_KeyOffset.setValue((int) (keySettings_m.getOffset() * 100));
		spinner_m_KeyOffset.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(listenerActive_m) {
					if(listenerActive_m) {
						keySettings_m.setOffset((double)(int)spinner_m_KeyOffset.getValue() / 100.0);
//						tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
						refreshKeyToWatermark();
						refreshWmToKeySettings();
						displayKeySettings();
					}
				}
			}
		});
		GridBagConstraints gbc_spinner_m_KeyOffset = new GridBagConstraints();
		gbc_spinner_m_KeyOffset.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner_m_KeyOffset.insets = new Insets(0, 0, 5, 5);
		gbc_spinner_m_KeyOffset.gridx = 1;
		gbc_spinner_m_KeyOffset.gridy = 0;
		panel_m_KeyInfo.add(spinner_m_KeyOffset, gbc_spinner_m_KeyOffset);
		
		JSeparator separator = new JSeparator();
		separator.setForeground(UIManager.getColor("Label.disabledForeground"));
		separator.setOrientation(SwingConstants.VERTICAL);
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.VERTICAL;
		gbc_separator.gridheight = 2;
		gbc_separator.insets = new Insets(0, 0, 5, 5);
		gbc_separator.gridx = 7;
		gbc_separator.gridy = 0;
		panel_m_KeyInfo.add(separator, gbc_separator);
		
		txt_m_KeyState = new JTextField();
		txt_m_KeyState.setText("Please select or generate a key");
		txt_m_KeyState.setBackground(UIManager.getColor("Panel.background"));
		txt_m_KeyState.setBorder(null);
		txt_m_KeyState.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize*5/6));
		GridBagConstraints gbc_txt_m_KeyState = new GridBagConstraints();
		gbc_txt_m_KeyState.gridwidth = 3;
		gbc_txt_m_KeyState.insets = new Insets(0, 0, 5, 0);
		gbc_txt_m_KeyState.fill = GridBagConstraints.BOTH;
		gbc_txt_m_KeyState.gridx = 8;
		gbc_txt_m_KeyState.gridy = 0;
		panel_m_KeyInfo.add(txt_m_KeyState, gbc_txt_m_KeyState);
		txt_m_KeyState.setColumns(10);
		
		btn_m_SaveKey = new JButton("Save Key");
		btn_m_SaveKey.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Watermark watermarkBackup = watermarkApply;
				refreshKeyToWatermark();
				if(watermarkApply.isNewKeySet()) {
					try {
						isReadiedToMark = false;
						watermarkApply.keyGen();
						File defaultDir = defaultResourcePath;
						if(wavIn_m != null) defaultDir = wavIn_m;
						if(keyFile_m != null) defaultDir = keyFile_m;
						File newKey = browseFile("Save Key", new String[] {"bin"}, defaultDir, true);
						if(newKey != null) {
							watermarkApply.saveKey(newKey);
							keyFile_m = newKey;
							watermarkApply.loadKey(keyFile_m);
							txt_m_KeyState.setText("Key saved and loaded: " + keyFile_m.getName());
							refreshWmToKeySettings();
							displayKeySettings();
							refreshIsReadiedToMark();
						} else watermarkApply = watermarkBackup;
					} catch (WatermarkException e1) {
						txt_m_KeyState.setText("Failed saving key: " + e1.getMessage());
						e1.printStackTrace();
					}
				} else {
					txt_m_KeyState.setText("Please load a watermark image! ");
				}
			}
		});
		btn_m_SaveKey.setBackground(buttonBgd);
		btn_m_SaveKey.setFont(new Font("µÈÏß Light", Font.BOLD, fontSize));
		btn_m_SaveKey.setToolTipText("Generate a new key with current settings and save it.");
		GridBagConstraints gbc_btn_m_SaveKey = new GridBagConstraints();
		gbc_btn_m_SaveKey.gridwidth = 2;
		gbc_btn_m_SaveKey.insets = new Insets(0, 0, 0, 5);
		gbc_btn_m_SaveKey.fill = GridBagConstraints.BOTH;
		gbc_btn_m_SaveKey.gridx = 0;
		gbc_btn_m_SaveKey.gridy = 1;
		panel_m_KeyInfo.add(btn_m_SaveKey, gbc_btn_m_SaveKey);
		
		Component horizontalStrut_2 = Box.createHorizontalStrut(10);
		GridBagConstraints gbc_horizontalStrut_2 = new GridBagConstraints();
		gbc_horizontalStrut_2.gridheight = 2;
		gbc_horizontalStrut_2.insets = new Insets(0, 0, 0, 5);
		gbc_horizontalStrut_2.gridx = 2;
		gbc_horizontalStrut_2.gridy = 0;
		panel_m_KeyInfo.add(horizontalStrut_2, gbc_horizontalStrut_2);
		
		JLabel lbl_m_KeyWidth = new JLabel("Key Width");
		lbl_m_KeyWidth.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_KeyWidth.setToolTipText("<html><p width=\"300\">The width of watermark specified by the key. "
				+ "If this width is defferent from that of the watermark image, the image will be resized to fit.</p></html>");
		GridBagConstraints gbc_lbl_m_KeyWidth = new GridBagConstraints();
		gbc_lbl_m_KeyWidth.anchor = GridBagConstraints.EAST;
		gbc_lbl_m_KeyWidth.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_m_KeyWidth.gridx = 3;
		gbc_lbl_m_KeyWidth.gridy = 0;
		panel_m_KeyInfo.add(lbl_m_KeyWidth, gbc_lbl_m_KeyWidth);
		
		spinner_m_KeyWidth = new JSpinner();
		spinner_m_KeyWidth.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		spinner_m_KeyWidth.setModel(new SpinnerNumberModel(256, Watermark.minWidth, Watermark.maxWidth, 2));
		spinner_m_KeyWidth.setValue(keySettings_m.getWidth());
		spinner_m_KeyWidth.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(listenerActive_m) {
					if(listenerActive_m) {
						int newWidth = (int)spinner_m_KeyWidth.getValue();
						double log2Width = Math.log(newWidth) / Math.log(2);
						double log2WidthInt = Math.round(log2Width);
						if(log2Width != log2WidthInt) {
							if(log2Width > log2WidthInt)
								newWidth = 1<<((int)log2WidthInt + 1);
							else
								newWidth = 1<<((int)log2WidthInt - 1);
						}
						keySettings_m.setWidth(newWidth);
//						tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
						refreshKeyToWatermark();
						refreshWmToKeySettings();
						displayKeySettings();
					}
				}
			}
		});
		GridBagConstraints gbc_spinner_m_KeyWidth = new GridBagConstraints();
		gbc_spinner_m_KeyWidth.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner_m_KeyWidth.insets = new Insets(0, 0, 5, 5);
		gbc_spinner_m_KeyWidth.gridx = 4;
		gbc_spinner_m_KeyWidth.gridy = 0;
		panel_m_KeyInfo.add(spinner_m_KeyWidth, gbc_spinner_m_KeyWidth);
		
		chckbx_m_Smooth = new JCheckBox("Smooth");
		chckbx_m_Smooth.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		chckbx_m_Smooth.setToolTipText("<html><p width=\"400\">Watermarking option. ONLY tick when marking "
				+ "audio without clear attacts (church organ, choir, natural noises like rain and wind noise "
				+ "etc.) and it is NOT recommended to use with audio with articulated sounds (speech, pluck, "
				+ "percussion etc.). In such cases the articularion of the sounds may get blurred.</p></html>");
		chckbx_m_Smooth.setSelected(keySettings_m.isSmooth());
		chckbx_m_Smooth.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(listenerActive_m) {
					keySettings_m.setSmooth(chckbx_m_Smooth.isSelected());
//					tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
					refreshKeyToWatermark();
					refreshWmToKeySettings();
					displayKeySettings();
				}
			}
		});
		GridBagConstraints gbc_chckbx_m_Smooth = new GridBagConstraints();
		gbc_chckbx_m_Smooth.anchor = GridBagConstraints.WEST;
		gbc_chckbx_m_Smooth.insets = new Insets(0, 0, 5, 5);
		gbc_chckbx_m_Smooth.gridx = 6;
		gbc_chckbx_m_Smooth.gridy = 0;
		panel_m_KeyInfo.add(chckbx_m_Smooth, gbc_chckbx_m_Smooth);
		
		chckbx_m_Robust = new JCheckBox("Robust");
		chckbx_m_Robust.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		chckbx_m_Robust.setToolTipText("<html><p width=\"400\">Watermarking option. In case that the audio "
				+ "is too short (<1 min), contains too much silence or only has a few non-empty frequency bands, "
				+ "tick ROBUST to increase the watermark robustivity without compromising the invisibility.</p></html>");
		chckbx_m_Robust.setSelected(keySettings_m.isRobust());
		chckbx_m_Robust.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(listenerActive_m) {
					keySettings_m.setRobust(chckbx_m_Robust.isSelected());
//					tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
					refreshKeyToWatermark();
					refreshWmToKeySettings();
					displayKeySettings();
				}
			}
		});
		GridBagConstraints gbc_chckbx_m_Robust = new GridBagConstraints();
		gbc_chckbx_m_Robust.anchor = GridBagConstraints.WEST;
		gbc_chckbx_m_Robust.insets = new Insets(0, 0, 0, 5);
		gbc_chckbx_m_Robust.gridx = 6;
		gbc_chckbx_m_Robust.gridy = 1;
		panel_m_KeyInfo.add(chckbx_m_Robust, gbc_chckbx_m_Robust);
		
		JLabel lbl_m_KeyHeight = new JLabel("Key Height");
		lbl_m_KeyHeight.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_KeyHeight.setToolTipText("<html><p width=\"300\">The height of watermark specified by the key. If this height is defferent from that of the watermark image, the image will be resized to fit.</p></html>");
		GridBagConstraints gbc_lbl_m_KeyHeight = new GridBagConstraints();
		gbc_lbl_m_KeyHeight.anchor = GridBagConstraints.EAST;
		gbc_lbl_m_KeyHeight.insets = new Insets(0, 0, 0, 5);
		gbc_lbl_m_KeyHeight.gridx = 3;
		gbc_lbl_m_KeyHeight.gridy = 1;
		panel_m_KeyInfo.add(lbl_m_KeyHeight, gbc_lbl_m_KeyHeight);
		
		spinner_m_KeyHeight = new JSpinner();
		spinner_m_KeyHeight.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		spinner_m_KeyHeight.setModel(new SpinnerNumberModel(256, Watermark.minHeight, Watermark.maxHeight, Watermark.heightStep));
		spinner_m_KeyHeight.setValue(keySettings_m.getHeight());
		spinner_m_KeyHeight.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(listenerActive_m) {
					keySettings_m.setHeight((int)spinner_m_KeyHeight.getValue());
//					tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
					refreshKeyToWatermark();
					refreshWmToKeySettings();
					displayKeySettings();
				}
			}
		});
		GridBagConstraints gbc_spinner_m_KeyHeight = new GridBagConstraints();
		gbc_spinner_m_KeyHeight.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner_m_KeyHeight.insets = new Insets(0, 0, 0, 5);
		gbc_spinner_m_KeyHeight.gridx = 4;
		gbc_spinner_m_KeyHeight.gridy = 1;
		panel_m_KeyInfo.add(spinner_m_KeyHeight, gbc_spinner_m_KeyHeight);
		
		JLabel lbl_m_KeyFold = new JLabel("Fold Number");
		lbl_m_KeyFold.setHorizontalAlignment(SwingConstants.RIGHT);
		lbl_m_KeyFold.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_m_KeyFold.setToolTipText("<html><p width=\"400\">Key property concerning the time domain. Describes how many times the original watermark image is horizontally folded before it is marked into audio. Recommended value: 2.</p></html>");
		GridBagConstraints gbc_lbl_m_KeyFold = new GridBagConstraints();
		gbc_lbl_m_KeyFold.anchor = GridBagConstraints.EAST;
		gbc_lbl_m_KeyFold.insets = new Insets(0, 0, 0, 5);
		gbc_lbl_m_KeyFold.gridx = 8;
		gbc_lbl_m_KeyFold.gridy = 1;
		panel_m_KeyInfo.add(lbl_m_KeyFold, gbc_lbl_m_KeyFold);
		
		spinner_m_FoldNum = new JSpinner();
		spinner_m_FoldNum.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(listenerActive_m) {
					keyFoldNumber_m = (int) spinner_m_FoldNum.getValue();
					keySettings_m.setFold(keyFoldNumber_m);
//					tmodel_m_WavIn.fireTableRowsUpdated(selRow, selRow);
					refreshKeyToWatermark();
					refreshWmToKeySettings();
					displayKeySettings();
				}
			}
		});
		spinner_m_FoldNum.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		spinner_m_FoldNum.setModel(new SpinnerNumberModel(2, 0, 8, 1));
		spinner_m_FoldNum.setValue(keyFoldNumber_m);
		GridBagConstraints gbc_spinner_m_FoldNum = new GridBagConstraints();
		gbc_spinner_m_FoldNum.fill = GridBagConstraints.HORIZONTAL;
		gbc_spinner_m_FoldNum.insets = new Insets(0, 0, 0, 5);
		gbc_spinner_m_FoldNum.gridx = 9;
		gbc_spinner_m_FoldNum.gridy = 1;
		panel_m_KeyInfo.add(spinner_m_FoldNum, gbc_spinner_m_FoldNum);
		
		btn_m_Abort = new JButton("Abort");
		btn_m_Abort.setEnabled(false);
		btn_m_Abort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				encode_flag.setGoOn(false);
				btn_m_Abort.setEnabled(false);
			}
		});
		btn_m_Abort.setBackground(buttonBgd);
		btn_m_Abort.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_Abort.setToolTipText("Immediately stop applying watermark.");
		GridBagConstraints gbc_btn_m_Abort = new GridBagConstraints();
		gbc_btn_m_Abort.fill = GridBagConstraints.BOTH;
		gbc_btn_m_Abort.insets = new Insets(0, 0, 5, 5);
		gbc_btn_m_Abort.gridx = 6;
		gbc_btn_m_Abort.gridy = 2;
		panel_m_R.add(btn_m_Abort, gbc_btn_m_Abort);
		
		JPanel panel_m_Play = new JPanel();
		GridBagConstraints gbc_panel_m_Play = new GridBagConstraints();
		gbc_panel_m_Play.insets = new Insets(0, 0, 5, 5);
		gbc_panel_m_Play.fill = GridBagConstraints.BOTH;
		gbc_panel_m_Play.gridx = 7;
		gbc_panel_m_Play.gridy = 2;
		panel_m_R.add(panel_m_Play, gbc_panel_m_Play);
		GridBagLayout gbl_panel_m_Play = new GridBagLayout();
		gbl_panel_m_Play.columnWidths = new int[]{30, 30, 30, 30};
		gbl_panel_m_Play.rowHeights = new int[]{30, 30};
		gbl_panel_m_Play.columnWeights = new double[]{1.0, 0.0, 0.0, 1.0};
		gbl_panel_m_Play.rowWeights = new double[]{1.0, 0.0};
		panel_m_Play.setLayout(gbl_panel_m_Play);
		
		JButton btn_m_PlayHome = new JButton("|<");
		btn_m_PlayHome.setBackground(buttonBgd);
		btn_m_PlayHome.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_m_PlayHome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		
		JLabel lblN_m_PlayTitle = new JLabel("Input Preview");
		lblN_m_PlayTitle.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lblN_m_PlayTitle = new GridBagConstraints();
		gbc_lblN_m_PlayTitle.gridwidth = 2;
		gbc_lblN_m_PlayTitle.insets = new Insets(0, 0, 5, 5);
		gbc_lblN_m_PlayTitle.gridx = 1;
		gbc_lblN_m_PlayTitle.gridy = 0;
		panel_m_Play.add(lblN_m_PlayTitle, gbc_lblN_m_PlayTitle);
		GridBagConstraints gbc_btn_m_PlayHome = new GridBagConstraints();
		gbc_btn_m_PlayHome.insets = new Insets(0, 0, 0, 5);
		gbc_btn_m_PlayHome.fill = GridBagConstraints.BOTH;
		gbc_btn_m_PlayHome.gridx = 1;
		gbc_btn_m_PlayHome.gridy = 1;
		panel_m_Play.add(btn_m_PlayHome, gbc_btn_m_PlayHome);
		
		JButton btn_m_PlayPause = new JButton(">");
		btn_m_PlayPause.setBackground(buttonBgd);
		btn_m_PlayPause.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_m_PlayPause = new GridBagConstraints();
		gbc_btn_m_PlayPause.fill = GridBagConstraints.BOTH;
		gbc_btn_m_PlayPause.insets = new Insets(0, 0, 0, 5);
		gbc_btn_m_PlayPause.gridx = 2;
		gbc_btn_m_PlayPause.gridy = 1;
		panel_m_Play.add(btn_m_PlayPause, gbc_btn_m_PlayPause);
		
		brModel_m = new DefaultBoundedRangeModel();
		progressBar_m = new JProgressBar(brModel_m);
		progressBar_m.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		progressBar_m.setToolTipText("Watermark reading progress");
		progressBar_m.setStringPainted(true);
		GridBagConstraints gbc_progressBar_m = new GridBagConstraints();
		gbc_progressBar_m.gridwidth = 6;
		gbc_progressBar_m.fill = GridBagConstraints.HORIZONTAL;
		gbc_progressBar_m.insets = new Insets(0, 0, 0, 5);
		gbc_progressBar_m.gridx = 1;
		gbc_progressBar_m.gridy = 3;
		panel_m_R.add(progressBar_m, gbc_progressBar_m);
		
		JSlider slider_1 = new JSlider();
		slider_1.setToolTipText("Cursor for playing the selected marked audio.");
		slider_1.setValue(0);
		GridBagConstraints gbc_slider_1 = new GridBagConstraints();
		gbc_slider_1.fill = GridBagConstraints.BOTH;
		gbc_slider_1.insets = new Insets(0, 0, 0, 5);
		gbc_slider_1.gridx = 7;
		gbc_slider_1.gridy = 3;
		panel_m_R.add(slider_1, gbc_slider_1);
		
		JPanel panel_reader = new JPanel();
		tabbedPane.addTab("Reader", null, panel_reader, null);
		GridBagLayout gbl_panel_reader = new GridBagLayout();
		gbl_panel_reader.columnWidths = new int[]{500};
		gbl_panel_reader.rowHeights = new int[]{150, 10, 252};
		gbl_panel_reader.columnWeights = new double[]{1.0};
		gbl_panel_reader.rowWeights = new double[]{0.0, 0.0, 1.0};
		panel_reader.setLayout(gbl_panel_reader);
		
		JLabel lbl_r_Title = new JLabel("");
		lbl_r_Title.setIcon(new ImageIcon("./res/LOGO_Spectromark_h120px.png"));
		GridBagConstraints gbc_lbl_r_Title = new GridBagConstraints();
		gbc_lbl_r_Title.insets = new Insets(0, 0, 5, 0);
		gbc_lbl_r_Title.gridx = 0;
		gbc_lbl_r_Title.gridy = 0;
		panel_reader.add(lbl_r_Title, gbc_lbl_r_Title);
		
		JSeparator separator_r_U = new JSeparator();
		separator_r_U.setForeground(UIManager.getColor("Label.disabledForeground"));
		GridBagConstraints gbc_separator_r_U = new GridBagConstraints();
		gbc_separator_r_U.insets = new Insets(0, 0, 5, 0);
		gbc_separator_r_U.gridx = 0;
		gbc_separator_r_U.gridy = 1;
		panel_reader.add(separator_r_U, gbc_separator_r_U);
		
		JPanel panel_r_Function = new JPanel();
		GridBagConstraints gbc_panel_r_Function = new GridBagConstraints();
		gbc_panel_r_Function.fill = GridBagConstraints.BOTH;
		gbc_panel_r_Function.gridx = 0;
		gbc_panel_r_Function.gridy = 2;
		panel_reader.add(panel_r_Function, gbc_panel_r_Function);
		GridBagLayout gbl_panel_r_Function = new GridBagLayout();
		gbl_panel_r_Function.columnWidths = new int[]{300, 10, 400, 10, 300};
		gbl_panel_r_Function.rowHeights = new int[]{0, 0, 0};
		gbl_panel_r_Function.columnWeights = new double[]{0.3, 0.0, 1.0, 0.0, 0.3};
		gbl_panel_r_Function.rowWeights = new double[]{0.0, 1.0, 0.0};
		panel_r_Function.setLayout(gbl_panel_r_Function);
		
		JPanel panel_r_FileIn = new JPanel();
		GridBagConstraints gbc_panel_r_FileIn = new GridBagConstraints();
		gbc_panel_r_FileIn.gridheight = 2;
		gbc_panel_r_FileIn.insets = new Insets(0, 0, 5, 5);
		gbc_panel_r_FileIn.fill = GridBagConstraints.BOTH;
		gbc_panel_r_FileIn.gridx = 0;
		gbc_panel_r_FileIn.gridy = 0;
		panel_r_Function.add(panel_r_FileIn, gbc_panel_r_FileIn);
		GridBagLayout gbl_panel_r_FileIn = new GridBagLayout();
		gbl_panel_r_FileIn.columnWidths = new int[]{0, 0, 0, 0, 0, 0};
		gbl_panel_r_FileIn.rowHeights = new int[]{0, 0, 10, 0, 0, 0, 0, 0, 0};
		gbl_panel_r_FileIn.columnWeights = new double[]{0.0, 1.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		gbl_panel_r_FileIn.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE};
		panel_r_FileIn.setLayout(gbl_panel_r_FileIn);
		
		JLabel lbl_r_WavIn = new JLabel("Wav File");
		lbl_r_WavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lbl_r_WavIn = new GridBagConstraints();
		gbc_lbl_r_WavIn.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_WavIn.gridx = 1;
		gbc_lbl_r_WavIn.gridy = 0;
		panel_r_FileIn.add(lbl_r_WavIn, gbc_lbl_r_WavIn);
		
		Component horizontalStrut_r_FileIn_L = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_FileIn_L = new GridBagConstraints();
		gbc_horizontalStrut_r_FileIn_L.gridheight = 5;
		gbc_horizontalStrut_r_FileIn_L.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_r_FileIn_L.gridx = 0;
		gbc_horizontalStrut_r_FileIn_L.gridy = 0;
		panel_r_FileIn.add(horizontalStrut_r_FileIn_L, gbc_horizontalStrut_r_FileIn_L);
		
		JTextPane txt_r_WavIn = new JTextPane();
		txt_r_WavIn.setEditable(false);
		txt_r_WavIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_txt_r_WavIn = new GridBagConstraints();
		gbc_txt_r_WavIn.gridwidth = 3;
		gbc_txt_r_WavIn.insets = new Insets(0, 0, 5, 5);
		gbc_txt_r_WavIn.fill = GridBagConstraints.BOTH;
		gbc_txt_r_WavIn.gridx = 1;
		gbc_txt_r_WavIn.gridy = 1;
		panel_r_FileIn.add(txt_r_WavIn, gbc_txt_r_WavIn);
		
		JButton btn_r_WavInSel = new JButton("Select...");
		btn_r_WavInSel.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_WavInSel = new GridBagConstraints();
		gbc_btn_r_WavInSel.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_WavInSel.gridx = 3;
		gbc_btn_r_WavInSel.gridy = 0;
		panel_r_FileIn.add(btn_r_WavInSel, gbc_btn_r_WavInSel);
		btn_r_WavInSel.setToolTipText("Select a marked audio file in *.wav format");
		btn_r_WavInSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File selectedWavFile;
				try {
					selectedWavFile = wavIn_r;
				} catch (Exception e1) {
					//wavIn not specified, use default path
					selectedWavFile = defaultResourcePath;
					//e1.printStackTrace();
				}
				selectedWavFile = browseFile("", new String[] {"wav"}, selectedWavFile, false);
				if(selectedWavFile == null) return;
				else wavIn_r = selectedWavFile;
				txt_r_message.setText("Wav file loaded: " + wavIn_r.getName());
				imageFilename = wavIn_r.getAbsolutePath().replaceFirst("[.][^.]+$", "");
				txt_r_Disp_SavePath.setText(imageFilename);
				
				txt_r_WavIn.setText(wavIn_r.getAbsolutePath());
	            refreshIsReadiedToRead();
			}
		});
		
		Component horizontalStrut_r_FileIn_R = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_FileIn_R = new GridBagConstraints();
		gbc_horizontalStrut_r_FileIn_R.gridheight = 5;
		gbc_horizontalStrut_r_FileIn_R.insets = new Insets(0, 0, 5, 0);
		gbc_horizontalStrut_r_FileIn_R.gridx = 4;
		gbc_horizontalStrut_r_FileIn_R.gridy = 0;
		panel_r_FileIn.add(horizontalStrut_r_FileIn_R, gbc_horizontalStrut_r_FileIn_R);
		
		JSeparator separator_r_FileIn_WavKey = new JSeparator();
		separator_r_FileIn_WavKey.setForeground(UIManager.getColor("Label.disabledForeground"));
		GridBagConstraints gbc_separator_r_FileIn_WavKey = new GridBagConstraints();
		gbc_separator_r_FileIn_WavKey.gridwidth = 3;
		gbc_separator_r_FileIn_WavKey.insets = new Insets(0, 0, 5, 5);
		gbc_separator_r_FileIn_WavKey.gridx = 1;
		gbc_separator_r_FileIn_WavKey.gridy = 2;
		panel_r_FileIn.add(separator_r_FileIn_WavKey, gbc_separator_r_FileIn_WavKey);
		
		JLabel lbl_r_KeyIn = new JLabel("Key File");
		lbl_r_KeyIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lbl_r_KeyIn = new GridBagConstraints();
		gbc_lbl_r_KeyIn.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_KeyIn.gridx = 1;
		gbc_lbl_r_KeyIn.gridy = 3;
		panel_r_FileIn.add(lbl_r_KeyIn, gbc_lbl_r_KeyIn);

		JTextPane txt_r_KeyIn = new JTextPane();
		txt_r_KeyIn.setText("");
		txt_r_KeyIn.setEditable(false);
		txt_r_KeyIn.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_txt_r_KeyIn = new GridBagConstraints();
		gbc_txt_r_KeyIn.insets = new Insets(0, 0, 5, 5);
		gbc_txt_r_KeyIn.gridwidth = 3;
		gbc_txt_r_KeyIn.fill = GridBagConstraints.BOTH;
		gbc_txt_r_KeyIn.gridx = 1;
		gbc_txt_r_KeyIn.gridy = 4;
		panel_r_FileIn.add(txt_r_KeyIn, gbc_txt_r_KeyIn);
		
		JButton btn_r_KeyInSel = new JButton("Select...");
		btn_r_KeyInSel.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_KeyInSel = new GridBagConstraints();
		gbc_btn_r_KeyInSel.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_KeyInSel.gridx = 3;
		gbc_btn_r_KeyInSel.gridy = 3;
		panel_r_FileIn.add(btn_r_KeyInSel, gbc_btn_r_KeyInSel);
		btn_r_KeyInSel.setToolTipText("Select a key file in *.bin format to decode the watermark");
		btn_r_KeyInSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File keyFile_backup = keyFile_r;
				if(keyFile_r == null && wavIn_r == null) {
					keyFile_r = browseFile("Key file *.bin", new String[] {"bin"}, defaultResourcePath, false);
				} else if(keyFile_r == null){
					keyFile_r = browseFile("Key file *.bin", new String[] {"bin"}, wavIn_r, false);
				} else {
					keyFile_r = browseFile("Key file *.bin", new String[] {"bin"}, keyFile_r, false);
				}
				if(keyFile_r != null) {
					Watermark wm_backup = watermarkRead;
					try {
						watermarkRead.loadKey(keyFile_r);
						txt_r_message.setText("Key loaded: " + keyFile_r.getName());
						txt_r_KeyIn.setText(keyFile_r.getAbsolutePath());
						refreshIsReadiedToRead();
					} catch(WatermarkException eWm) {
						keyFile_r = keyFile_backup;
						watermarkRead = wm_backup;
						txt_r_message.setText("Invalid key! Current key unchanged.");
					}
				} else keyFile_r = keyFile_backup;
			}
		});
		
		JLabel lbl_r_FileInDrop = new JLabel("Drop Wav & Key here");
		lbl_r_FileInDrop.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_r_FileInDrop.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_lbl_r_FileInDrop = new GridBagConstraints();
		gbc_lbl_r_FileInDrop.fill = GridBagConstraints.BOTH;
		gbc_lbl_r_FileInDrop.insets = new Insets(0, 0, 5, 0);
		gbc_lbl_r_FileInDrop.gridwidth = 5;
		gbc_lbl_r_FileInDrop.gridx = 0;
		gbc_lbl_r_FileInDrop.gridy = 5;
		panel_r_FileIn.add(lbl_r_FileInDrop, gbc_lbl_r_FileInDrop);
		lbl_r_FileInDrop.setDropTarget(new DropTarget() {
		    /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			public synchronized void drop(DropTargetDropEvent evt) {
		        try {
		            evt.acceptDrop(DnDConstants.ACTION_COPY);
		            List<File> droppedWmFiles = (List<File>)
		                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
		            for (File file : droppedWmFiles) {
		            	String filename = file.getName();
		                if(filename.endsWith(".wav")) {
		                	wavIn_r = file;
		                	txt_r_WavIn.setText(wavIn_r.getAbsolutePath());
		                	imageFilename = wavIn_r.getAbsolutePath().replaceFirst("[.][^.]+$", "");
		                	txt_r_Disp_SavePath.setText(imageFilename);
		                }
		                else if(filename.endsWith(".bin")) {
		                	File keyFile_backup = keyFile_r;
		                	keyFile_r = file;
		                	Watermark wm_backup = watermarkRead;
	    					try {
	    						watermarkRead.loadKey(keyFile_r);
	    						txt_r_message.setText("Key loaded: " + keyFile_r.getName());
	    						txt_r_KeyIn.setText(keyFile_r.getAbsolutePath());
	    					} catch(WatermarkException eWm) {
	    						keyFile_r = keyFile_backup;
	    						watermarkRead = wm_backup;
	    						txt_r_message.setText("Invalid key! Current key unchanged.");
	    					}
		                }  // else do nothing
		            }
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		        refreshIsReadiedToRead();
		    }
		});
		
		brModel_r = new DefaultBoundedRangeModel();
		
		txt_r_message = new JTextField();
		txt_r_message.setHorizontalAlignment(SwingConstants.CENTER);
		txt_r_message.setEditable(false);
		txt_r_message.setText("Please load a wav file and a key.");
		txt_r_message.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize*5/6));
		txt_r_message.setBorder(null);
		GridBagConstraints gbc_textField = new GridBagConstraints();
		gbc_textField.gridwidth = 3;
		gbc_textField.insets = new Insets(0, 0, 5, 5);
		gbc_textField.fill = GridBagConstraints.HORIZONTAL;
		gbc_textField.gridx = 1;
		gbc_textField.gridy = 6;
		panel_r_FileIn.add(txt_r_message, gbc_textField);
		txt_r_message.setColumns(10);
		
		progressBar_r = new JProgressBar(brModel_r);
		progressBar_r.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		progressBar_r.setToolTipText("Watermarking progress");
		progressBar_r.setStringPainted(true);
		GridBagConstraints gbc_progressBar = new GridBagConstraints();
		gbc_progressBar.fill = GridBagConstraints.HORIZONTAL;
		gbc_progressBar.gridwidth = 3;
		gbc_progressBar.insets = new Insets(0, 0, 0, 5);
		gbc_progressBar.gridx = 1;
		gbc_progressBar.gridy = 7;
		panel_r_FileIn.add(progressBar_r, gbc_progressBar);
		
		JSplitPane splitPane_r_mid = new JSplitPane();
		splitPane_r_mid.setContinuousLayout(true);
		splitPane_r_mid.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane_r_mid.setResizeWeight(0.485);
		GridBagConstraints gbc_splitPane_r_mid = new GridBagConstraints();
		gbc_splitPane_r_mid.gridheight = 2;
		gbc_splitPane_r_mid.insets = new Insets(5, 5, 5, 5);
		gbc_splitPane_r_mid.fill = GridBagConstraints.BOTH;
		gbc_splitPane_r_mid.gridx = 2;
		gbc_splitPane_r_mid.gridy = 0;
		panel_r_Function.add(splitPane_r_mid, gbc_splitPane_r_mid);
		
		JPanel panel_r_midlow = new JPanel();
		splitPane_r_mid.setRightComponent(panel_r_midlow);
		GridBagLayout gbl_panel_r_midlow = new GridBagLayout();
		gbl_panel_r_midlow.columnWidths = new int[]{0, 0};
		gbl_panel_r_midlow.rowHeights = new int[]{0, 0};
		gbl_panel_r_midlow.columnWeights = new double[]{1.0, Double.MIN_VALUE};
		gbl_panel_r_midlow.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		panel_r_midlow.setLayout(gbl_panel_r_midlow);
		
		JLabel lbl_r_res2 = new JLabel("");
		lbl_r_res2.setMinimumSize(new Dimension(1, 1));
		lbl_r_res2.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lbl_r_res2 = new GridBagConstraints();
		gbc_lbl_r_res2.fill = GridBagConstraints.BOTH;
		gbc_lbl_r_res2.gridx = 0;
		gbc_lbl_r_res2.gridy = 0;
		panel_r_midlow.add(lbl_r_res2, gbc_lbl_r_res2);
		
		JPanel panel_r_midtop = new JPanel();
		splitPane_r_mid.setLeftComponent(panel_r_midtop);
		GridBagLayout gbl_panel_r_midtop = new GridBagLayout();
		gbl_panel_r_midtop.columnWidths = new int[]{10};
		gbl_panel_r_midtop.rowHeights = new int[]{10};
		gbl_panel_r_midtop.columnWeights = new double[]{1.0};
		gbl_panel_r_midtop.rowWeights = new double[]{1.0};
		panel_r_midtop.setLayout(gbl_panel_r_midtop);
		
		JLabel lbl_r_res1 = new JLabel("");
		lbl_r_res1.setHorizontalAlignment(SwingConstants.CENTER);
		lbl_r_res1.setMinimumSize(new Dimension(1, 1));
		GridBagConstraints gbc_lbl_r_res1 = new GridBagConstraints();
		gbc_lbl_r_res1.fill = GridBagConstraints.BOTH;
		gbc_lbl_r_res1.gridx = 0;
		gbc_lbl_r_res1.gridy = 0;
		panel_r_midtop.add(lbl_r_res1, gbc_lbl_r_res1);
		
		ComponentListener l_r_res = new ComponentListener() {
			@Override
			public void componentHidden(ComponentEvent evt) {
			}
			@Override
			public void componentShown(ComponentEvent evt) {
			}
			@Override
			public void componentMoved(ComponentEvent evt) {
			}
			@Override
			public void componentResized(ComponentEvent evt) {
				try {
					if(icon_r_res1.getImage() == null || icon_r_res2.getImage() == null) return;
					lbl_r_res1.setIcon(fitImageonLabel(toBufferedImage(icon_r_res1.getImage()), lbl_r_res1));
					lbl_r_res2.setIcon(fitImageonLabel(toBufferedImage(icon_r_res2.getImage()), lbl_r_res2));
				} catch (NullPointerException e){
					System.out.println("Failed to display watermark images!");
				}
			}
		};
		lbl_r_res1.addComponentListener(l_r_res);
		lbl_r_res2.addComponentListener(l_r_res);
		
		JSeparator separator_r_LL = new JSeparator();
		separator_r_LL.setForeground(UIManager.getColor("Label.disabledForeground"));
		GridBagConstraints gbc_separator_r_LL = new GridBagConstraints();
		gbc_separator_r_LL.gridheight = 3;
		gbc_separator_r_LL.insets = new Insets(0, 0, 0, 5);
		gbc_separator_r_LL.gridx = 1;
		gbc_separator_r_LL.gridy = 0;
		panel_r_Function.add(separator_r_LL, gbc_separator_r_LL);
		
		JSeparator separator_r_LR = new JSeparator();
		separator_r_LR.setForeground(UIManager.getColor("Label.disabledForeground"));
		GridBagConstraints gbc_separator_r_LR = new GridBagConstraints();
		gbc_separator_r_LR.gridheight = 3;
		gbc_separator_r_LR.insets = new Insets(0, 0, 0, 5);
		gbc_separator_r_LR.gridx = 3;
		gbc_separator_r_LR.gridy = 0;
		panel_r_Function.add(separator_r_LR, gbc_separator_r_LR);
		
		JPanel panel_r_DispParam = new JPanel();
		GridBagConstraints gbc_panel_r_DispParam = new GridBagConstraints();
		gbc_panel_r_DispParam.insets = new Insets(0, 0, 5, 0);
		gbc_panel_r_DispParam.gridheight = 2;
		gbc_panel_r_DispParam.fill = GridBagConstraints.BOTH;
		gbc_panel_r_DispParam.gridx = 4;
		gbc_panel_r_DispParam.gridy = 0;
		panel_r_Function.add(panel_r_DispParam, gbc_panel_r_DispParam);
		GridBagLayout gbl_panel_r_DispParam = new GridBagLayout();
		gbl_panel_r_DispParam.columnWidths = new int[]{0, 30, 0, 30, 0, 0};
		gbl_panel_r_DispParam.rowHeights = new int[]{0, 0, 0, 0, 0};
		gbl_panel_r_DispParam.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0};
		gbl_panel_r_DispParam.rowWeights = new double[]{0.05, 0.0, 0.0, 1.0};
		panel_r_DispParam.setLayout(gbl_panel_r_DispParam);
		
		JButton btn_r_Abort = new JButton("Abort");
		btn_r_Abort.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_r_Abort.setEnabled(false);
		GridBagConstraints gbc_btn_r_Abort = new GridBagConstraints();
		gbc_btn_r_Abort.fill = GridBagConstraints.BOTH;
		gbc_btn_r_Abort.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_Abort.gridx = 3;
		gbc_btn_r_Abort.gridy = 0;
		panel_r_DispParam.add(btn_r_Abort, gbc_btn_r_Abort);
		btn_r_Abort.setToolTipText("Interrupt the watermark reading process");
		btn_r_Abort.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				decode_flag.setGoOn(false);
				btn_r_Abort.setEnabled(false);
				progressBar_r.setString("Aborted");
			}
		});
		
		Component horizontalStrut_r_DispParam_R = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_DispParam_R = new GridBagConstraints();
		gbc_horizontalStrut_r_DispParam_R.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_r_DispParam_R.gridheight = 2;
		gbc_horizontalStrut_r_DispParam_R.gridx = 4;
		gbc_horizontalStrut_r_DispParam_R.gridy = 0;
		panel_r_DispParam.add(horizontalStrut_r_DispParam_R, gbc_horizontalStrut_r_DispParam_R);
		
		Component horizontalStrut_r_DispParam_L = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_DispParam_L = new GridBagConstraints();
		gbc_horizontalStrut_r_DispParam_L.gridheight = 2;
		gbc_horizontalStrut_r_DispParam_L.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_r_DispParam_L.gridx = 0;
		gbc_horizontalStrut_r_DispParam_L.gridy = 0;
		panel_r_DispParam.add(horizontalStrut_r_DispParam_L, gbc_horizontalStrut_r_DispParam_L);
		
		JLabel lbl_r_Disp_Save = new JLabel("Save Image");
		lbl_r_Disp_Save.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lbl_r_Disp_Save = new GridBagConstraints();
		gbc_lbl_r_Disp_Save.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_Save.gridx = 1;
		gbc_lbl_r_Disp_Save.gridy = 1;
		panel_r_DispParam.add(lbl_r_Disp_Save, gbc_lbl_r_Disp_Save);
		
		txt_r_Disp_SavePath = new JTextPane();
		txt_r_Disp_SavePath.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_txt_r_Disp_SavePath = new GridBagConstraints();
		gbc_txt_r_Disp_SavePath.insets = new Insets(0, 0, 5, 5);
		gbc_txt_r_Disp_SavePath.fill = GridBagConstraints.BOTH;
		gbc_txt_r_Disp_SavePath.gridx = 2;
		gbc_txt_r_Disp_SavePath.gridy = 1;
		panel_r_DispParam.add(txt_r_Disp_SavePath, gbc_txt_r_Disp_SavePath);
		
		JButton btn_r_Disp_SaveBrowse = new JButton("Browse...");
		btn_r_Disp_SaveBrowse.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_Disp_SaveBrowse = new GridBagConstraints();
		gbc_btn_r_Disp_SaveBrowse.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_Disp_SaveBrowse.gridx = 3;
		gbc_btn_r_Disp_SaveBrowse.gridy = 1;
		panel_r_DispParam.add(btn_r_Disp_SaveBrowse, gbc_btn_r_Disp_SaveBrowse);
		btn_r_Disp_SaveBrowse.setToolTipText("Select a reference filename for the decoded watermark image files");
		btn_r_Disp_SaveBrowse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File imageFile = browseFile("Save Watermark Image", null, wavIn_r, true);
				if(imageFile == null) return;
				imageFilename = imageFile.getAbsolutePath().replaceFirst("[.][^.]+$", "");
				txt_r_Disp_SavePath.setText(imageFilename);
			}
		});
		
		JButton btn_r_Disp_Save = new JButton("Save Image");
		btn_r_Disp_Save.setEnabled(false);
		btn_r_Disp_Save.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_Disp_Save = new GridBagConstraints();
		gbc_btn_r_Disp_Save.fill = GridBagConstraints.BOTH;
		gbc_btn_r_Disp_Save.gridwidth = 3;
		gbc_btn_r_Disp_Save.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_Disp_Save.gridx = 1;
		gbc_btn_r_Disp_Save.gridy = 2;
		panel_r_DispParam.add(btn_r_Disp_Save, gbc_btn_r_Disp_Save);
		btn_r_Disp_Save.setToolTipText("Save the decoded watermark image as *.png files");
		btn_r_Disp_Save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(watermarkRead.isKeyReadied()) {
					try {
						if(imageFilename == null) return;
						String imagefilename1 = imageFilename + "1.png";
						String imagefilename2 = imageFilename + "2.png";
						Image image;
						image = icon_r_res1.getImage();
						Watermark.saveImage(toBufferedImage(image), new File(imagefilename1));
						image = icon_r_res2.getImage();
						Watermark.saveImage(toBufferedImage(image), new File(imagefilename2));
						txt_r_message.setText("Results saved!");
					} catch (WatermarkException e1) {
						txt_r_message.setText("Failed saving decoded watermark: " + e1.getMessage());
						e1.printStackTrace();
					}
				} else {
					txt_r_message.setText("Please load a wav file and a key! ");
				}
			}
		});
		
		JScrollPane scrollPane_r_DispParam = new JScrollPane();
		GridBagConstraints gbc_scrollPane_r_DispParam = new GridBagConstraints();
		gbc_scrollPane_r_DispParam.gridwidth = 3;
		gbc_scrollPane_r_DispParam.insets = new Insets(0, 0, 5, 5);
		gbc_scrollPane_r_DispParam.fill = GridBagConstraints.BOTH;
		gbc_scrollPane_r_DispParam.gridx = 1;
		gbc_scrollPane_r_DispParam.gridy = 3;
		panel_r_DispParam.add(scrollPane_r_DispParam, gbc_scrollPane_r_DispParam);
		
		JPanel panel_r_disp_pars = new JPanel();
		scrollPane_r_DispParam.setViewportView(panel_r_disp_pars);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{10, 10, 10, 10, 10};
		gbl_panel.rowHeights = new int[]{30, 30, 30, 10, 30, 30, 30, 30, 30};
		gbl_panel.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0};
		gbl_panel.rowWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0};
		panel_r_disp_pars.setLayout(gbl_panel);
		
		JLabel lbl_r_Disp_Smooth = new JLabel("Smooth");
		lbl_r_Disp_Smooth.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lbl_r_Disp_Smooth = new GridBagConstraints();
		gbc_lbl_r_Disp_Smooth.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_Smooth.gridx = 1;
		gbc_lbl_r_Disp_Smooth.gridy = 0;
		panel_r_disp_pars.add(lbl_r_Disp_Smooth, gbc_lbl_r_Disp_Smooth);
		
		Component horizontalStrut_r_Disp_Scr_L = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_Disp_Scr_L = new GridBagConstraints();
		gbc_horizontalStrut_r_Disp_Scr_L.gridheight = 5;
		gbc_horizontalStrut_r_Disp_Scr_L.insets = new Insets(0, 0, 5, 5);
		gbc_horizontalStrut_r_Disp_Scr_L.gridx = 0;
		gbc_horizontalStrut_r_Disp_Scr_L.gridy = 0;
		panel_r_disp_pars.add(horizontalStrut_r_Disp_Scr_L, gbc_horizontalStrut_r_Disp_Scr_L);
		
		Component horizontalStrut_r_Disp_Scr_R = Box.createHorizontalStrut(20);
		GridBagConstraints gbc_horizontalStrut_r_Disp_Scr_R = new GridBagConstraints();
		gbc_horizontalStrut_r_Disp_Scr_R.gridheight = 5;
		gbc_horizontalStrut_r_Disp_Scr_R.insets = new Insets(0, 0, 5, 0);
		gbc_horizontalStrut_r_Disp_Scr_R.gridx = 4;
		gbc_horizontalStrut_r_Disp_Scr_R.gridy = 0;
		panel_r_disp_pars.add(horizontalStrut_r_Disp_Scr_R, gbc_horizontalStrut_r_Disp_Scr_R);
		
		JLabel lbl_r_Disp_SmoothRad = new JLabel("Radius");
		lbl_r_Disp_SmoothRad.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_SmoothRad.setToolTipText("Smoothing / Blurring radius of the decoded watermark image");
		GridBagConstraints gbc_lbl_r_Disp_SmoothRad = new GridBagConstraints();
		gbc_lbl_r_Disp_SmoothRad.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_SmoothRad.gridx = 1;
		gbc_lbl_r_Disp_SmoothRad.gridy = 1;
		panel_r_disp_pars.add(lbl_r_Disp_SmoothRad, gbc_lbl_r_Disp_SmoothRad);
		
		JSlider slider_r_SmoothRad = new JSlider();
		slider_r_SmoothRad.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});
		slider_r_SmoothRad.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_SmoothRad = new GridBagConstraints();
		gbc_slider_r_SmoothRad.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_SmoothRad.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_SmoothRad.gridwidth = 2;
		gbc_slider_r_SmoothRad.gridx = 2;
		gbc_slider_r_SmoothRad.gridy = 1;
		panel_r_disp_pars.add(slider_r_SmoothRad, gbc_slider_r_SmoothRad);
		slider_r_SmoothRad.setMinorTickSpacing(1);
		slider_r_SmoothRad.setMajorTickSpacing(20);
		slider_r_SmoothRad.setPaintLabels(true);
		slider_r_SmoothRad.setSnapToTicks(true);
		slider_r_SmoothRad.setValue(10);
		
		JLabel lbl_r_Disp_SmoothKern = new JLabel("Kernel Size");
		lbl_r_Disp_SmoothKern.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_SmoothKern.setToolTipText("Larger kernel size may result in better images but slower run");
		GridBagConstraints gbc_lbl_r_Disp_SmoothKern = new GridBagConstraints();
		gbc_lbl_r_Disp_SmoothKern.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_SmoothKern.gridx = 1;
		gbc_lbl_r_Disp_SmoothKern.gridy = 2;
		panel_r_disp_pars.add(lbl_r_Disp_SmoothKern, gbc_lbl_r_Disp_SmoothKern);
		
		JSlider slider_r_SmoothKern = new JSlider();
		slider_r_SmoothKern.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_SmoothKern = new GridBagConstraints();
		gbc_slider_r_SmoothKern.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_SmoothKern.gridwidth = 2;
		gbc_slider_r_SmoothKern.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_SmoothKern.gridx = 2;
		gbc_slider_r_SmoothKern.gridy = 2;
		panel_r_disp_pars.add(slider_r_SmoothKern, gbc_slider_r_SmoothKern);
		slider_r_SmoothKern.setMinorTickSpacing(1);
		slider_r_SmoothKern.setMinimum(0);
		slider_r_SmoothKern.setMaximum(100);
		slider_r_SmoothKern.setMajorTickSpacing(20);
		slider_r_SmoothKern.setPaintTicks(false);
		slider_r_SmoothKern.setSnapToTicks(true);
		slider_r_SmoothKern.setPaintTrack(true);
		slider_r_SmoothKern.setPaintLabels(true);
		slider_r_SmoothKern.setValue(50);
		slider_r_SmoothKern.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});
		
		JSeparator separator_r_SmoothTone = new JSeparator();
		GridBagConstraints gbc_separator_r_SmoothTone = new GridBagConstraints();
		gbc_separator_r_SmoothTone.gridwidth = 3;
		gbc_separator_r_SmoothTone.insets = new Insets(0, 0, 5, 5);
		gbc_separator_r_SmoothTone.gridx = 1;
		gbc_separator_r_SmoothTone.gridy = 3;
		panel_r_disp_pars.add(separator_r_SmoothTone, gbc_separator_r_SmoothTone);
		separator_r_SmoothTone.setForeground(UIManager.getColor("Label.disabledForeground"));
		
		JLabel lbl_r_Tone = new JLabel("Tone");
		lbl_r_Tone.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_lbl_r_Tone = new GridBagConstraints();
		gbc_lbl_r_Tone.insets = new Insets(0, 0, 0, 5);
		gbc_lbl_r_Tone.gridx = 1;
		gbc_lbl_r_Tone.gridy = 4;
		panel_r_disp_pars.add(lbl_r_Tone, gbc_lbl_r_Tone);

		JLabel lbl_r_Disp_Bright = new JLabel("Brightness");
		lbl_r_Disp_Bright.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_Bright.setToolTipText("Brightness of the decoded watermark image");
		GridBagConstraints gbc_lbl_r_Disp_Bright = new GridBagConstraints();
		gbc_lbl_r_Disp_Bright.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_Bright.gridx = 1;
		gbc_lbl_r_Disp_Bright.gridy = 5;
		panel_r_disp_pars.add(lbl_r_Disp_Bright, gbc_lbl_r_Disp_Bright);
		
		JSlider slider_r_Bright = new JSlider();
		slider_r_Bright.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_Bright = new GridBagConstraints();
		gbc_slider_r_Bright.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_Bright.gridwidth = 2;
		gbc_slider_r_Bright.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_Bright.gridx = 2;
		gbc_slider_r_Bright.gridy = 5;
		panel_r_disp_pars.add(slider_r_Bright, gbc_slider_r_Bright);
		slider_r_Bright.setMinorTickSpacing(1);
		slider_r_Bright.setMinimum(0);
		slider_r_Bright.setMaximum(100);
		slider_r_Bright.setMajorTickSpacing(20);
		slider_r_Bright.setPaintTicks(false);
		slider_r_Bright.setSnapToTicks(true);
		slider_r_Bright.setPaintTrack(true);
		slider_r_Bright.setPaintLabels(true);
		slider_r_Bright.setValue(50);
		slider_r_Bright.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});
		
		JLabel lbl_r_Disp_Contrast = new JLabel("Contrast");
		lbl_r_Disp_Contrast.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_Contrast.setToolTipText("Contrast of the decoded watermark image");
		GridBagConstraints gbc_lbl_r_Disp_Contrast = new GridBagConstraints();
		gbc_lbl_r_Disp_Contrast.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_Contrast.gridx = 1;
		gbc_lbl_r_Disp_Contrast.gridy = 6;
		panel_r_disp_pars.add(lbl_r_Disp_Contrast, gbc_lbl_r_Disp_Contrast);
		
		JSlider slider_r_Contrast = new JSlider();
		slider_r_Contrast.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_Contrast = new GridBagConstraints();
		gbc_slider_r_Contrast.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_Contrast.gridwidth = 2;
		gbc_slider_r_Contrast.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_Contrast.gridx = 2;
		gbc_slider_r_Contrast.gridy = 6;
		panel_r_disp_pars.add(slider_r_Contrast, gbc_slider_r_Contrast);
		slider_r_Contrast.setMinorTickSpacing(1);
		slider_r_Contrast.setMinimum(0);
		slider_r_Contrast.setMaximum(100);
		slider_r_Contrast.setMajorTickSpacing(20);
		slider_r_Contrast.setPaintTicks(false);
		slider_r_Contrast.setSnapToTicks(true);
		slider_r_Contrast.setPaintTrack(true);
		slider_r_Contrast.setPaintLabels(true);
		slider_r_Contrast.setValue(75);
		slider_r_Contrast.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});
		
		JLabel lbl_r_Disp_ColorTemp = new JLabel("Color Temp");
		lbl_r_Disp_ColorTemp.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_ColorTemp.setToolTipText("Color temperature of the decoded watermark image, lower->blue, higher->yellow");
		GridBagConstraints gbc_lbl_r_Disp_ColorTemp = new GridBagConstraints();
		gbc_lbl_r_Disp_ColorTemp.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_ColorTemp.gridx = 1;
		gbc_lbl_r_Disp_ColorTemp.gridy = 7;
		panel_r_disp_pars.add(lbl_r_Disp_ColorTemp, gbc_lbl_r_Disp_ColorTemp);
		
		JSlider slider_r_ColorTemp = new JSlider();
		slider_r_ColorTemp.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_ColorTemp = new GridBagConstraints();
		gbc_slider_r_ColorTemp.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_ColorTemp.gridwidth = 2;
		gbc_slider_r_ColorTemp.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_ColorTemp.gridx = 2;
		gbc_slider_r_ColorTemp.gridy = 7;
		panel_r_disp_pars.add(slider_r_ColorTemp, gbc_slider_r_ColorTemp);
		slider_r_ColorTemp.setMinorTickSpacing(1);
		slider_r_ColorTemp.setMinimum(0);
		slider_r_ColorTemp.setMaximum(100);
		slider_r_ColorTemp.setMajorTickSpacing(20);
		slider_r_ColorTemp.setPaintTicks(false);
		slider_r_ColorTemp.setSnapToTicks(true);
		slider_r_ColorTemp.setPaintTrack(true);
		slider_r_ColorTemp.setPaintLabels(true);
		slider_r_ColorTemp.setValue(40);
		slider_r_ColorTemp.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});
		
		JLabel lbl_r_Disp_Tint = new JLabel("Tint");
		lbl_r_Disp_Tint.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		lbl_r_Disp_Tint.setToolTipText("Tint of the decoded watermark image, lower->green, higher->magenta");
		GridBagConstraints gbc_lbl_r_Disp_Tint = new GridBagConstraints();
		gbc_lbl_r_Disp_Tint.insets = new Insets(0, 0, 5, 5);
		gbc_lbl_r_Disp_Tint.gridx = 1;
		gbc_lbl_r_Disp_Tint.gridy = 8;
		panel_r_disp_pars.add(lbl_r_Disp_Tint, gbc_lbl_r_Disp_Tint);
		
		JSlider slider_r_Tint = new JSlider();
		slider_r_Tint.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.PLAIN, fontSize));
		GridBagConstraints gbc_slider_r_Tint = new GridBagConstraints();
		gbc_slider_r_Tint.insets = new Insets(0, 0, 5, 5);
		gbc_slider_r_Tint.gridwidth = 2;
		gbc_slider_r_Tint.fill = GridBagConstraints.HORIZONTAL;
		gbc_slider_r_Tint.gridx = 2;
		gbc_slider_r_Tint.gridy = 8;
		panel_r_disp_pars.add(slider_r_Tint, gbc_slider_r_Tint);
		slider_r_Tint.setMinorTickSpacing(1);
		slider_r_Tint.setMinimum(0);
		slider_r_Tint.setMaximum(100);
		slider_r_Tint.setMajorTickSpacing(20);
		slider_r_Tint.setPaintTicks(false);
		slider_r_Tint.setSnapToTicks(true);
		slider_r_Tint.setPaintTrack(true);
		slider_r_Tint.setPaintLabels(true);
		slider_r_Tint.setValue(45);
		slider_r_Tint.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				if(btn_r_Refresh == null) return;
				btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
			}
		});

		JButton btn_r_Disp_SmoothDefault = new JButton("Default");
		btn_r_Disp_SmoothDefault.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_Disp_SmoothDefault = new GridBagConstraints();
		gbc_btn_r_Disp_SmoothDefault.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_Disp_SmoothDefault.gridx = 3;
		gbc_btn_r_Disp_SmoothDefault.gridy = 0;
		panel_r_disp_pars.add(btn_r_Disp_SmoothDefault, gbc_btn_r_Disp_SmoothDefault);
		btn_r_Disp_SmoothDefault.setToolTipText("Reset the smoothing parameters");
		btn_r_Disp_SmoothDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider_r_SmoothRad.setValue(10);
				slider_r_SmoothKern.setValue(50);
			}
		});
		
		JButton btn_r_ToneDefault = new JButton("Default");
		btn_r_ToneDefault.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		GridBagConstraints gbc_btn_r_ToneDefault = new GridBagConstraints();
		gbc_btn_r_ToneDefault.insets = new Insets(0, 0, 0, 5);
		gbc_btn_r_ToneDefault.gridx = 3;
		gbc_btn_r_ToneDefault.gridy = 4;
		panel_r_disp_pars.add(btn_r_ToneDefault, gbc_btn_r_ToneDefault);
		btn_r_ToneDefault.setToolTipText("Reset the toning parameters");
		btn_r_ToneDefault.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				slider_r_Bright.setValue(50);
				slider_r_Contrast.setValue(75);
				slider_r_ColorTemp.setValue(40);
				slider_r_Tint.setValue(45);
			}
		});
		
		btn_r_Refresh = new JButton("Refresh");
//		btn_r_Refresh.setEnabled(false);
//		btn_r_Refresh.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
//		GridBagConstraints gbc_btn_r_Refresh = new GridBagConstraints();
//		gbc_btn_r_Refresh.fill = GridBagConstraints.BOTH;
//		gbc_btn_r_Refresh.insets = new Insets(0, 0, 5, 5);
//		gbc_btn_r_Refresh.gridx = 3;
//		gbc_btn_r_Refresh.gridy = 2;
//		panel_r_DispParam.add(btn_r_Refresh, gbc_btn_r_Refresh);  // THIS BUTTON IS NOW HIDDEN
		btn_r_Refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(array_r_res_mask.getMask(1) == null) return;
				blur = new VisWmBlur(slider_r_SmoothRad.getValue() / 100.0 * 
						(VisWmBlur.maxGausBlurRad - VisWmBlur.minGausBlurRad) + VisWmBlur.minGausBlurRad, 
						slider_r_SmoothKern.getValue() / 100.0 * 
						(VisWmBlur.maxCoreWidth_BlurRad_Ratio - VisWmBlur.minCoreWidth_BlurRad_Ratio) + VisWmBlur.minCoreWidth_BlurRad_Ratio);
				color = new VisWmColor(slider_r_Bright.getValue() * 0.02 - 1.0, 
						slider_r_Contrast.getValue() * 0.02 - 1.0, 
						slider_r_ColorTemp.getValue() * 0.02 - 1.0, 
						slider_r_Tint.getValue() * 0.02 - 1.0);
				icon_r_res1.setImage(watermarkRead.maskToImage(array_r_res_mask.getMask(1), blur, color));
				icon_r_res2.setImage(watermarkRead.maskToImage(array_r_res_mask.getMask(2), blur, color));
				l_r_res.componentResized(new ComponentEvent(lbl_r_res1, 0));
			}
		});

		btn_r_ReadWm = new JButton("Read Watermark");
		btn_r_ReadWm.setFont(new Font("Î¢ÈíÑÅºÚ Light", Font.BOLD, fontSize));
		btn_r_ReadWm.setEnabled(false);
		GridBagConstraints gbc_btn_r_ReadWatermark = new GridBagConstraints();
		gbc_btn_r_ReadWatermark.fill = GridBagConstraints.BOTH;
		gbc_btn_r_ReadWatermark.gridwidth = 2;
		gbc_btn_r_ReadWatermark.insets = new Insets(0, 0, 5, 5);
		gbc_btn_r_ReadWatermark.gridx = 1;
		gbc_btn_r_ReadWatermark.gridy = 0;
		panel_r_DispParam.add(btn_r_ReadWm, gbc_btn_r_ReadWatermark);
		btn_r_ReadWm.setToolTipText("Read the watermark from the audio file with the key");
		btn_r_ReadWm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new Runnable() {
			        public void run() {
			        	ReadWatermark readWm;
			        	decode_flag.setGoOn(true);
			        	btn_r_Abort.setEnabled(true);
			        	progressBar_r.setString(null);//Display the percentage
						readWm = new ReadWatermark(wavIn_r, keyFile_r, false);
						//Google: Java task / JProgressbar
						//https://docs.oracle.com/javase/9/docs/api/index.html?javafx/concurrent/Task.html
						//https://docs.oracle.com/javase/7/docs/api/javax/swing/JProgressBar.html
						//https://docs.oracle.com/javase/tutorial/uiswing/components/progress.html
						readWm.decode(brModel_r, decode_flag, false, array_r_res_mask);
//						btn_r_Refresh.setEnabled(true);
						btn_r_Refresh.getActionListeners()[0].actionPerformed(new ActionEvent(btn_r_Refresh, 0, ""));
						txt_r_message.setText("Reading complete.");
						btn_r_Abort.setEnabled(false);
						btn_r_Disp_Save.setEnabled(true);
						return;
			        }
			    });
				t.start();
			}
		});
		
		Component verticalStrut_r_L = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_r_L = new GridBagConstraints();
		gbc_verticalStrut_r_L.gridwidth = 5;
		gbc_verticalStrut_r_L.gridx = 0;
		gbc_verticalStrut_r_L.gridy = 2;
		panel_r_Function.add(verticalStrut_r_L, gbc_verticalStrut_r_L);
		
//		JPanel panel_About = new JPanel();
//		tabbedPane.addTab("About", null, panel_About, null);
		
		frmSpectromark.setTitle("Spectromark");
		frmSpectromark.setBounds(100, 100, 1400, 600);
		frmSpectromark.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	private ImageIcon fitImageonLabel(BufferedImage img, JLabel lbl) {
		ImageIcon rtn = null;
		if(img != null) {
			double aRImg = (double)img.getHeight() / img.getWidth();//aR = aspectRatio
			double aRLbl = (double)lbl.getHeight() / lbl.getWidth();
			double zoom = 1.0;
			if(aRImg > aRLbl) zoom = (double)lbl.getHeight() / img.getHeight();
			else zoom = (double)lbl.getWidth() / img.getWidth();
			Image newwm = img.getScaledInstance((int)((double)img.getWidth() * zoom), 
					(int)((double)img.getHeight() * zoom), Image.SCALE_SMOOTH);
			rtn = new ImageIcon(newwm);
		}
		else throw new NullPointerException("GUI.fitImageInLabel: Null BufferedImage img.");
		return rtn;
	}
	
	private void refreshIsReadiedToMark() {
		if(wavInList.size() == 0) return;
		int numSelected = 0;
		for(int i=0; i<wavInList.size(); i++) {
			if(wavInList.get(i).isSelected()) {
				numSelected += 1;
				break;
			}
		}
		if(numSelected > 0) {
			isReadiedToMark = watermarkApply.isImgReadied() && watermarkApply.isNewKeySet();
		} else isReadiedToMark = false;
		btn_m_ApplyWm.setEnabled(isReadiedToMark);
	}
	
	private void refreshIsReadiedToRead() {
		isReadiedToRead = wavIn_r.isFile() && watermarkRead.isKeyReadied();
		btn_r_ReadWm.setEnabled(isReadiedToRead);
	}
	
	private void refreshWmToKeySettings() {//read settings from watermark to keySettings
		keySettings_m.setOffset(watermarkApply.getOffset());
		keySettings_m.setWidth(watermarkApply.getWidth());
		keySettings_m.setHeight(watermarkApply.getHeight());
		keySettings_m.setSmooth(watermarkApply.isModeSmooth());
		keySettings_m.setRobust(watermarkApply.isModeRobust());
	}
	
	private void refreshKeyToWatermark() {//apply keySettings to watermark
//		watermarkApply.setOffset(keySettings.getOffset());
//		watermarkApply.setSize((short) keySettings.getWidth(), (short) keySettings.getWidth());
//		watermarkApply.setMode(keySettings.isSmooth(), keySettings.isRobust());
		watermarkApply.setKey(keySettings_m);
	}
	
	private void displayKeySettings() {
		listenerActive_m = false;
		spinner_m_KeyOffset.setValue((int) (keySettings_m.getOffset() * 100));
		spinner_m_KeyWidth.setValue(keySettings_m.getWidth());
		spinner_m_KeyHeight.setValue(keySettings_m.getHeight());
		chckbx_m_Smooth.setSelected(keySettings_m.isSmooth());
		chckbx_m_Robust.setSelected(keySettings_m.isRobust());
		listenerActive_m = true;
	}
	
	private File browseFile(String title, String acceptedExtensions[], File defaultFile, boolean falseToOpen_TrueToSave) {
		File selectedFile = null;
		final JFileChooser chooser = new JFileChooser();
		if(selectedFile == null)
		{
			if(defaultFile != null) chooser.setCurrentDirectory(defaultFile.getParentFile());
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter;
			if(acceptedExtensions == null) acceptedExtensions = new String[] {};
			if(acceptedExtensions.length == 1) {
				filter = new FileNameExtensionFilter(
						"*."+acceptedExtensions[0], acceptedExtensions[0]);
				chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			} else if(acceptedExtensions.length >= 2) {
				String filterName = "";
				for(int i=0; i<acceptedExtensions.length; i++) {
					filterName += ("."+acceptedExtensions[i]+" ");
				}
				filter = new FileNameExtensionFilter(
						filterName, acceptedExtensions);
				chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			} else {
				// do nothing, so no filter is applied if acceptedExtensions is an empty array {}
			}
			
			if(falseToOpen_TrueToSave) {
				if(chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION ){
			    	selectedFile = chooser.getSelectedFile();
			    }
			} else {
				if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
			    	selectedFile = chooser.getSelectedFile();
			    }
			}
			
//			if(selectedFile == null) {
//				int ans_INT = JOptionPane.showConfirmDialog (null, 
//						"Do you wish to select a file or a folder?",
//						"File Not Selected", JOptionPane.YES_NO_OPTION);
//				if(ans_INT == JOptionPane.NO_OPTION) return null;
//			}
		}
		return selectedFile;
	}
	
	private File[] browseFiles(String title, String[] acceptedExtensions, File defaultFile) {
		File[] selectedFile = null;
		final JFileChooser chooser = new JFileChooser();
		if(selectedFile == null)
		{
			chooser.setMultiSelectionEnabled(true);
			chooser.setCurrentDirectory(defaultFile.getParentFile());
			chooser.setDialogTitle(title);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter;
			if(acceptedExtensions.length == 1) {
				filter = new FileNameExtensionFilter(
						"*."+acceptedExtensions[0], acceptedExtensions[0]);
				chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			} else if(acceptedExtensions.length >= 2) {
				String filterName = "";
				for(int i=0; i<acceptedExtensions.length; i++) {
					filterName += ("."+acceptedExtensions[i]+" ");
				}
				filter = new FileNameExtensionFilter(
						filterName, acceptedExtensions);
				chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			} else {
				// do nothing, so no filter is applied if acceptedExtensions is an empty array {}
			}
			
			if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
		    	selectedFile = chooser.getSelectedFiles();
		    }
			
//			if(selectedFile == null) {
//				int ans_INT = JOptionPane.showConfirmDialog (null, 
//						"Do you wish to select a file or a folder?",
//						"File Not Selected", JOptionPane.YES_NO_OPTION);
//				if(ans_INT == JOptionPane.NO_OPTION) return new File[] {null};
//			}
		}
		return selectedFile;
	}
	
	/*
	 * https://stackoverflow.com/questions/3954616/java-look-and-feel-lf
	 * answered Jul 18 '15 at 15:42 by WIll
	public void changeLookAndFeel() {
        List<String> lookAndFeelsDisplay = new ArrayList<>();
        List<String> lookAndFeelsRealNames = new ArrayList<>();

        for (LookAndFeelInfo each : UIManager.getInstalledLookAndFeels()) {
            lookAndFeelsDisplay.add(each.getName());
            lookAndFeelsRealNames.add(each.getClassName());
        }

        String changeLook = (String) JOptionPane.showInputDialog(this, "Choose Look and Feel Here:", "Select Look and Feel", JOptionPane.QUESTION_MESSAGE, null, lookAndFeelsDisplay.toArray(), null);

        if (changeLook != null) {
            for (int i = 0; i < lookAndFeelsDisplay.size(); i++) {
                if (changeLook.equals(lookAndFeelsDisplay.get(i))) {
                    try {
                        UIManager.setLookAndFeel(lookAndFeelsRealNames.get(i));
                        break;
                    }
                    catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                        err.println(ex);
                        ex.printStackTrace(System.err);
                    }
                }
            }
        }
    }*/
	
}
