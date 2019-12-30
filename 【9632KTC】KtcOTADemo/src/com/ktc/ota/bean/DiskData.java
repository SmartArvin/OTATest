package com.ktc.ota.bean;

/**
 * @author Arvin
 * @TODO USB磁盘信息
 * @date 2019.1.24
 */
public class DiskData{

	private String path ;
	private String name ;
	
	public DiskData() {
		super();
	}

	public DiskData(String path, String name) {
		super();
		this.path = path;
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
