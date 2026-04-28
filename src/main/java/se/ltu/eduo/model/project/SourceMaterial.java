package se.ltu.eduo.model.project;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A file uploaded to a project, stored in full in the database. Keeping the
 * file bytes in PostgreSQL is appropriate for prototype scale; query methods
 * must never select {@code fileData} unless the bytes are actually needed —
 * use a projection or a dedicated fetch method instead.
 */
@Entity
@Table(name = "source_material", schema = "eduo")
@Getter
@Setter
@NoArgsConstructor
public class SourceMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "source_material_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "filename", nullable = false)
    private String filename;

    /**
     * MIME type or informal extension label, e.g. {@code "application/pdf"}.
     * Used by the frontend to display the file type without reading the bytes.
     */
    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    /**
     * Raw file bytes. Intentionally not included in any derived query — always
     * fetch explicitly via a dedicated repository method.
     */
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "file_data", nullable = false)
    private byte[] fileData;

    @CreationTimestamp
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    public SourceMaterial(Project project, String filename, String fileType, byte[] fileData) {
        this.project = project;
        this.filename = filename;
        this.fileType = fileType;
        this.fileData = fileData;
        this.fileSizeBytes = fileData.length;
    }
}
