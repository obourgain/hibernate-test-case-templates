package org.hibernate.bugs;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@IdClass(ShopTaxAssociation.ShopTaxAssociationsId.class)
public class ShopTaxAssociation {

    private Shop shop;
    private UUID taxId;

    public ShopTaxAssociation() {
    }

    public ShopTaxAssociation(Shop shop, UUID taxId) {
        this.shop = shop;
        this.taxId = taxId;
    }

    @Id
    @ManyToOne
    @JoinColumn(name = "shop_id", referencedColumnName = "id")
    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }

    @Id
    @Column(name = "tax_uuid")
    public UUID getTaxId() {
        return taxId;
    }

    public void setTaxId(UUID taxId) {
        this.taxId = taxId;
    }

    static class ShopTaxAssociationsId implements Serializable {

        private Shop shop;
        private UUID taxId;

        public Shop getShop() {
            return shop;
        }

        public void setShop(Shop shop) {
            this.shop = shop;
        }

        public UUID getTaxId() {
            return taxId;
        }

        public void setTaxId(UUID taxId) {
            this.taxId = taxId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ShopTaxAssociationsId that = (ShopTaxAssociationsId) o;
            return Objects.equals(shop.getId(), that.shop.getId()) && Objects.equals(taxId, that.taxId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(shop.getId(), taxId);
        }
    }
}
