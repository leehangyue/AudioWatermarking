package gui;

import java.io.IOException;
import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

import pre_posts.WavFile;
import pre_posts.WavFileException;

public class WavTaskTableModel extends AbstractTableModel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4554474864531315433L;
	private final String[] columnNames = {"Selected", "Name", "Length", "Input Path"};
	private ArrayList<WavTask> wavInList;
	
	public WavTaskTableModel(ArrayList<WavTask> wavInList) {
		this.wavInList = wavInList;
	}
	
	public void refreshTable(ArrayList<WavTask> wavInList) {
		this.wavInList = wavInList;
	}
	
	@Override
    public String getColumnName(int column)
    {
        return columnNames[column];
    }
	
	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public int getRowCount() {
		return wavInList.size();
	}

	@Override
	public boolean isCellEditable(int row, int col) {
        //Note that the data/cell address is constant,
        //no matter where the cell appears on screen.
        if (col >= 1 && col <= 3) {//Name, Length, InputPath not editable
            return false;
        } else {//Selected, OutputPath (use specified output path or not) editable
            return true;
        }
    }
	
	@Override
	public Object getValueAt(int row, int column) {
		WavTask tsk = wavInList.get(row);
		if(column == 0) return tsk.isSelected();
		if(column == 1) return tsk.getFile().getName();
		if(column == 2) {
			try {
				WavFile wav = new WavFile(tsk.getFile());
				return new AudioDuration((double)wav.getNumFrames()/(double)wav.getSampleRate());
			} catch (IOException e) {
				System.out.println("WavTaskTableModel: WavFile - Error reading wavFile (IOException)");
				e.printStackTrace();
			} catch (WavFileException e) {
				System.out.println("WavTaskTableModel: WavFile - Incompatible wav format (WavFileException)");
				e.printStackTrace();
			}
		}
		if(column == 3) return tsk.getFile().getParent();
		return null;
	}
	
	@Override
	public void setValueAt(Object newValue, int row, int column) {
		//super.setValueAt(aValue, rowIndex, columnIndex); by default empty implementation is not necesary if direct parent is AbstractTableModel
		WavTask tsk = wavInList.get(row);
		if(column == 0) {//Is selected boolean
			tsk.setSelected((boolean)newValue);
			wavInList.set(row, tsk);
		}// else the cell is not editable
		fireTableCellUpdated(row, column);// notify listeners
	}
	
	@Override
    public Class<?> getColumnClass(int column)
    {
        return getValueAt(0, column).getClass();
    }

}
