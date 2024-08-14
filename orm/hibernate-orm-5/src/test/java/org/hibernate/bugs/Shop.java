package org.hibernate.bugs;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

import static javax.persistence.CascadeType.ALL;

@Entity
public class Shop {

    private Long id;
    private List<ShopTaxAssociation> shopTaxAssociations = new ArrayList<>();

    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "shop", cascade = ALL)
    public List<ShopTaxAssociation> getShopTaxAssociations() {
        return shopTaxAssociations;
    }

    public void setShopTaxAssociations(List<ShopTaxAssociation> shopTaxAssociations) {
        this.shopTaxAssociations = shopTaxAssociations;
    }

}
