package com.example.searchtestdevice.contact;

public class ContactBean {
	int id;
	String name;
	String phoneNum;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getPhoneNum() {
		return phoneNum;
	}
	public void setPhoneNum(String phoneNum) {
		this.phoneNum = phoneNum;
	}
	@Override
	public String toString() {
		return "ContactBean [id=" + id + ", name=" + name + ", phoneNum="
				+ phoneNum + "]";
	}
	
}
