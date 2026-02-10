package searchengine.model;

import lombok.Data;

import javax.persistence.*;
import javax.persistence.Index;

@Entity
@Table(name = "pages", indexes = {@Index(name = "path_index", columnList = "path")})
@Data
public class Page {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "site_id")
    private Integer siteId;

    @Column(columnDefinition = "VARCHAR(512)", nullable = false, unique = true)
    private String path;

    @Column(nullable = false)
    private Integer code;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    @ManyToOne
    @JoinColumn(name = "site_id", nullable = false, insertable = false, updatable = false)
    private SiteInfo siteInfo;
}
