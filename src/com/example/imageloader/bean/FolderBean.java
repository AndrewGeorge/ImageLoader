package com.example.imageloader.bean;

public class FolderBean {

	private String dir;
	private String firstImagPath;
	private String name;
	private int count;
	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
		
		int lastIndexOf=this.dir.lastIndexOf("/");
		this.name=this.dir.substring(lastIndexOf+1);
	}
	public String getFirstImagPath() {
		return firstImagPath;
	}
	public void setFirstImagPath(String firstImagPath) {
		this.firstImagPath = firstImagPath;
	}
	public String getName() {
		return name;
	}
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	
	
}
