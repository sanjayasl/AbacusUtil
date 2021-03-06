/*
 * Generated by Abacus.
 */
package com.landawn.samples.abacus.entity;

import java.sql.Timestamp;
import com.landawn.abacus.util.N;
import com.landawn.abacus.annotation.Type;


/**
 * Generated by Abacus.
 * @version ${version}
 */
public class Contact {
    private long id;
    private long accountId;
    private String mobile;
    private String telephone;
    private String email;
    private String address;
    private String address2;
    private String city;
    private String state;
    private String country;
    private String zipCode;
    private String category;
    private String description;
    private int status;
    private Timestamp lastUpdateTime;
    private Timestamp createTime;

    public Contact() {
    }

    public Contact(long id) {
        this();

        setId(id);
    }

    public Contact(long accountId, String mobile, String telephone, String email, 
        String address, String address2, String city, String state, 
        String country, String zipCode, String category, String description, 
        int status, Timestamp lastUpdateTime, Timestamp createTime) {
        this();

        setAccountId(accountId);
        setMobile(mobile);
        setTelephone(telephone);
        setEmail(email);
        setAddress(address);
        setAddress2(address2);
        setCity(city);
        setState(state);
        setCountry(country);
        setZipCode(zipCode);
        setCategory(category);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreateTime(createTime);
    }

    public Contact(long id, long accountId, String mobile, String telephone, String email, 
        String address, String address2, String city, String state, 
        String country, String zipCode, String category, String description, 
        int status, Timestamp lastUpdateTime, Timestamp createTime) {
        this();

        setId(id);
        setAccountId(accountId);
        setMobile(mobile);
        setTelephone(telephone);
        setEmail(email);
        setAddress(address);
        setAddress2(address2);
        setCity(city);
        setState(state);
        setCountry(country);
        setZipCode(zipCode);
        setCategory(category);
        setDescription(description);
        setStatus(status);
        setLastUpdateTime(lastUpdateTime);
        setCreateTime(createTime);
    }

    @Type("long")
    public long getId() {
        return id;
    }

    public Contact setId(long id) {
        this.id = id;

        return this;
    }

    @Type("long")
    public long getAccountId() {
        return accountId;
    }

    public Contact setAccountId(long accountId) {
        this.accountId = accountId;

        return this;
    }

    @Type("String")
    public String getMobile() {
        return mobile;
    }

    public Contact setMobile(String mobile) {
        this.mobile = mobile;

        return this;
    }

    @Type("String")
    public String getTelephone() {
        return telephone;
    }

    public Contact setTelephone(String telephone) {
        this.telephone = telephone;

        return this;
    }

    @Type("String")
    public String getEmail() {
        return email;
    }

    public Contact setEmail(String email) {
        this.email = email;

        return this;
    }

    @Type("String")
    public String getAddress() {
        return address;
    }

    public Contact setAddress(String address) {
        this.address = address;

        return this;
    }

    @Type("String")
    public String getAddress2() {
        return address2;
    }

    public Contact setAddress2(String address2) {
        this.address2 = address2;

        return this;
    }

    @Type("String")
    public String getCity() {
        return city;
    }

    public Contact setCity(String city) {
        this.city = city;

        return this;
    }

    @Type("String")
    public String getState() {
        return state;
    }

    public Contact setState(String state) {
        this.state = state;

        return this;
    }

    @Type("String")
    public String getCountry() {
        return country;
    }

    public Contact setCountry(String country) {
        this.country = country;

        return this;
    }

    @Type("String")
    public String getZipCode() {
        return zipCode;
    }

    public Contact setZipCode(String zipCode) {
        this.zipCode = zipCode;

        return this;
    }

    @Type("String")
    public String getCategory() {
        return category;
    }

    public Contact setCategory(String category) {
        this.category = category;

        return this;
    }

    @Type("String")
    public String getDescription() {
        return description;
    }

    public Contact setDescription(String description) {
        this.description = description;

        return this;
    }

    @Type("int")
    public int getStatus() {
        return status;
    }

    public Contact setStatus(int status) {
        this.status = status;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getLastUpdateTime() {
        return lastUpdateTime;
    }

    public Contact setLastUpdateTime(Timestamp lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;

        return this;
    }

    @Type("Timestamp")
    public Timestamp getCreateTime() {
        return createTime;
    }

    public Contact setCreateTime(Timestamp createTime) {
        this.createTime = createTime;

        return this;
    }

    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(id);
        h = 31 * h + N.hashCode(accountId);
        h = 31 * h + N.hashCode(mobile);
        h = 31 * h + N.hashCode(telephone);
        h = 31 * h + N.hashCode(email);
        h = 31 * h + N.hashCode(address);
        h = 31 * h + N.hashCode(address2);
        h = 31 * h + N.hashCode(city);
        h = 31 * h + N.hashCode(state);
        h = 31 * h + N.hashCode(country);
        h = 31 * h + N.hashCode(zipCode);
        h = 31 * h + N.hashCode(category);
        h = 31 * h + N.hashCode(description);
        h = 31 * h + N.hashCode(status);
        h = 31 * h + N.hashCode(lastUpdateTime);
        h = 31 * h + N.hashCode(createTime);

        return h;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Contact) {
            final Contact other = (Contact) obj;

            return N.equals(id, other.id)
                && N.equals(accountId, other.accountId)
                && N.equals(mobile, other.mobile)
                && N.equals(telephone, other.telephone)
                && N.equals(email, other.email)
                && N.equals(address, other.address)
                && N.equals(address2, other.address2)
                && N.equals(city, other.city)
                && N.equals(state, other.state)
                && N.equals(country, other.country)
                && N.equals(zipCode, other.zipCode)
                && N.equals(category, other.category)
                && N.equals(description, other.description)
                && N.equals(status, other.status)
                && N.equals(lastUpdateTime, other.lastUpdateTime)
                && N.equals(createTime, other.createTime);
        }

        return false;
    }

    public String toString() {
         return "{id=" + N.toString(id)
                 + ", accountId=" + N.toString(accountId)
                 + ", mobile=" + N.toString(mobile)
                 + ", telephone=" + N.toString(telephone)
                 + ", email=" + N.toString(email)
                 + ", address=" + N.toString(address)
                 + ", address2=" + N.toString(address2)
                 + ", city=" + N.toString(city)
                 + ", state=" + N.toString(state)
                 + ", country=" + N.toString(country)
                 + ", zipCode=" + N.toString(zipCode)
                 + ", category=" + N.toString(category)
                 + ", description=" + N.toString(description)
                 + ", status=" + N.toString(status)
                 + ", lastUpdateTime=" + N.toString(lastUpdateTime)
                 + ", createTime=" + N.toString(createTime)
                 + "}";
    }
}
