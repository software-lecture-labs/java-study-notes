package com.lectures.business.design.sales.persistence;

import com.lectures.business.design.sales.domain.Customer;
import com.lectures.business.design.sales.domain.CustomerId;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity {

    @Id
    @Column(name = "customer_id", length = 5)
    private String customerId;

    @Column(name = "company_name", nullable = false, length = 40)
    private String companyName;

    @Column(name = "contact_name", length = 30)
    private String contactName;

    @Embedded
    private AddressColumns address;

    protected CustomerEntity() {
    }

    static CustomerEntity from(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.customerId = customer.customerId().value();
        entity.companyName = customer.companyName();
        entity.contactName = customer.contactName().orElse(null);
        entity.address = AddressColumns.from(customer.address());
        return entity;
    }

    Customer toDomain() {
        Customer customer = new Customer(CustomerId.of(customerId), companyName, address.toAddress());
        if (contactName != null) {
            customer.assignContact(contactName);
        }
        return customer;
    }
}
