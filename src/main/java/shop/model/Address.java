package shop.model;

/* 배송지 (address) */
public class Address {
	private int addrId, memberId;
	private String addrName, receiver, receiverPhone, zipcode, addr1, addr2, isDefault;

	public int getAddrId() { return addrId; }
	public int getMemberId() { return memberId; }
	public String getAddrName() { return addrName; }
	public String getReceiver() { return receiver; }
	public String getReceiverPhone() { return receiverPhone; }
	public String getZipcode() { return zipcode; }
	public String getAddr1() { return addr1; }
	public String getAddr2() { return addr2; }
	public String getIsDefault() { return isDefault; }
}
