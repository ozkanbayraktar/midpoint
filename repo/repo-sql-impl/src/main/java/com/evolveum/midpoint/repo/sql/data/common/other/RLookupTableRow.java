package com.evolveum.midpoint.repo.sql.data.common.other;

import com.evolveum.midpoint.repo.sql.data.common.RLookupTable;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.id.RLookupTableRowId;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.sql.Timestamp;

/**
 * @author Viliam Repan (lazyman)
 */
//@Entity
//@Table(indexes = {
//todo create indexes after lookup api is created (when we know how we will search through lookup table [lazyman]
//        @Index(name = "i_row_key", columnList = "key"),
//        @Index(name = "i_row_label_orig", columnList = "label.orig"),
//        @Index(name = "i_row_label_norm", columnList = "label.norm")
//})
//@IdClass(RLookupTableRowId.class)
public class RLookupTableRow {

    private RLookupTable owner;
    private String ownerOid;

    private String key;
    private String value;
    private RPolyString label;
    private XMLGregorianCalendar lastChangeTimestamp;

    @Id
    @ForeignKey(name = "fk_lookup_table")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    public RLookupTable getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "row_key")
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public RPolyString getLabel() {
        return label;
    }

    public void setLabel(RPolyString label) {
        this.label = label;
    }

    public XMLGregorianCalendar getLastChangeTimestamp() {
        return lastChangeTimestamp;
    }

    public void setLastChangeTimestamp(XMLGregorianCalendar lastChangeTimestamp) {
        this.lastChangeTimestamp = lastChangeTimestamp;
    }

    @Column(name = "row_value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setOwner(RLookupTable owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RLookupTableRow that = (RLookupTableRow) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (label != null ? !label.equals(that.label) : that.label != null) return false;
        if (lastChangeTimestamp != null ? !lastChangeTimestamp.equals(that.lastChangeTimestamp) : that.lastChangeTimestamp != null)
            return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        result = 31 * result + (label != null ? label.hashCode() : 0);
        result = 31 * result + (lastChangeTimestamp != null ? lastChangeTimestamp.hashCode() : 0);
        return result;
    }
}