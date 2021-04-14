package com.github.komarovd95.widgetstore.application.repository;

import com.github.komarovd95.widgetstore.application.domain.Region;
import com.github.komarovd95.widgetstore.application.domain.Widget;
import com.github.komarovd95.widgetstore.application.service.transaction.TransactionsService;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * The implementation of the {@link WidgetsRepository} backed by H2 Database.
 * <p>
 * For search queries and overlying widgets shifting, the implementation relies on B-tree indexes
 * (columns {@code widget_id} and {@code z_index}).
 * <p>
 * It uses SQL standard statements like {@code MERGE INTO} for updates.
 * <p>
 * For spatial search, the implementation uses H2's {@code GEOMETRY} type and built-in spatial index.
 * <p>
 * Insert and update methods execute several SQL DML statements, therefore, these methods MUST be called in database
 * transaction.
 *
 * @see TransactionsService
 */
public class H2DatabaseWidgetsRepository implements WidgetsRepository {

    private static final RowMapper<Widget> ROW_MAPPER = (resultSet, i) -> Widget.builder()
        .setId(resultSet.getString("widget_id"))
        .setBoundaries(
            Region.builder()
                .setX(resultSet.getInt("x"))
                .setY(resultSet.getInt("y"))
                .setWidth(resultSet.getInt("width"))
                .setHeight(resultSet.getInt("height"))
                .builder()
        )
        .setZ(resultSet.getInt("z_index"))
        .setModifiedAt(resultSet.getTimestamp("modified_at").toInstant())
        .build();

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public H2DatabaseWidgetsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    }

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Integer> getCurrentForegroundZIndex() {
        Integer currentForegroundZIndex = DataAccessUtils.singleResult(jdbcTemplate.queryForList(
            "SELECT max(z_index) FROM widget",
            Collections.emptyMap(),
            Integer.class
        ));
        return Optional.ofNullable(currentForegroundZIndex);
    }

    /**
     * @inheritDocs
     */
    @Override
    public void insert(String id, Region boundaries, int zIndex, Instant modificationTimestamp) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(boundaries, "boundaries");
        Objects.requireNonNull(modificationTimestamp, "modificationTimestamp");

        shiftOverlyingWidgets(zIndex, modificationTimestamp);

        jdbcTemplate.update(
            "INSERT INTO widget (widget_id, x, y, width, height, z_index, boundaries, modified_at) " +
                "VALUES (:widget_id, :x, :y, :width, :height, :z_index, :boundaries, :modified_at)",
            new MapSqlParameterSource()
                .addValue("widget_id", id)
                .addValue("x", boundaries.getX())
                .addValue("y", boundaries.getY())
                .addValue("width", boundaries.getWidth())
                .addValue("height", boundaries.getHeight())
                .addValue("z_index", zIndex)
                .addValue("boundaries", toPolygon(boundaries))
                .addValue("modified_at", modificationTimestamp)
        );
    }

    /**
     * @inheritDocs
     */
    @Override
    public void update(Widget widget, Region newBoundaries, int newZIndex, Instant modificationTimestamp) {
        Objects.requireNonNull(widget, "widget");
        Objects.requireNonNull(newBoundaries, "newBoundaries");
        Objects.requireNonNull(modificationTimestamp, "modificationTimestamp");

        if (widget.getZ() != newZIndex) {
            deleteById(widget.getId());
            shiftOverlyingWidgets(newZIndex, modificationTimestamp);
        }

        jdbcTemplate.update(
            "MERGE INTO widget USING DUAL ON widget_id = :widget_id " +
                "WHEN MATCHED THEN UPDATE SET " +
                    "x = :x, " +
                    "y = :y, " +
                    "width = :width, " +
                    "height = :height, " +
                    "z_index = :z_index, " +
                    "boundaries = :boundaries, " +
                    "modified_at = :modified_at " +
                "WHEN NOT MATCHED THEN INSERT (widget_id, x, y, width, height, z_index, boundaries, modified_at) " +
                    "VALUES (:widget_id, :x, :y, :width, :height, :z_index, :boundaries, :modified_at)",
            new MapSqlParameterSource()
                .addValue("widget_id", widget.getId())
                .addValue("x", newBoundaries.getX())
                .addValue("y", newBoundaries.getY())
                .addValue("width", newBoundaries.getWidth())
                .addValue("height", newBoundaries.getHeight())
                .addValue("z_index", newZIndex)
                .addValue("boundaries", toPolygon(newBoundaries))
                .addValue("modified_at", modificationTimestamp)
        );
    }

    private void shiftOverlyingWidgets(int zIndex, Instant modificationTimestamp) {
        List<Long> idsToUpdate = jdbcTemplate.queryForList(
            "WITH RECURSIVE cte(id, z_index) AS (" +
                "SELECT id, z_index FROM widget WHERE z_index = :z_index " +
                "UNION ALL " +
                "SELECT widget.id, widget.z_index " +
                "FROM widget INNER JOIN cte ON widget.z_index = cte.z_index + 1 " +
                ")" +
                "SELECT id FROM cte ORDER BY z_index DESC",
            new MapSqlParameterSource()
                .addValue("z_index", zIndex),
            Long.class
        );

        jdbcTemplate.batchUpdate(
            "UPDATE widget " +
                "SET z_index = z_index + 1, modified_at = :modified_at " +
                "WHERE id = :id",
            idsToUpdate.stream()
                .map(recordId -> new MapSqlParameterSource()
                    .addValue("id", recordId)
                    .addValue("modified_at", modificationTimestamp)
                )
                .toArray(MapSqlParameterSource[]::new)
        );
    }

    /**
     * @inheritDocs
     */
    @Override
    public boolean deleteById(String id) {
        Objects.requireNonNull(id, "id");
        int deletedRows = jdbcTemplate.update(
            "DELETE FROM widget WHERE widget_id = :id",
            new MapSqlParameterSource()
                .addValue("id", id)
        );
        return deletedRows == 1;
    }

    /**
     * @inheritDocs
     */
    @Override
    public Optional<Widget> getWidgetById(String id) {
        Objects.requireNonNull(id, "id");

        Widget widget = DataAccessUtils.singleResult(jdbcTemplate.query(
            "SELECT widget_id, x, y, width, height, z_index, modified_at " +
                "FROM widget " +
                "WHERE widget_id = :id",
            new MapSqlParameterSource()
                .addValue("id", id),
            ROW_MAPPER
        ));
        return Optional.ofNullable(widget);
    }

    /**
     * @inheritDocs
     */
    @Override
    public List<Widget> getWidgets(Region regionToSearch, Integer zIndexCursor, int limit) {
        StringJoiner clauses = new StringJoiner(" AND ");
        clauses.add("1 = 1");
        MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("limit", limit);
        if (regionToSearch != null) {
            clauses
                .add("boundaries && :boundaries") // for index usage
                .add("x >= :x")
                .add("y >= :y")
                .add("(x + width) <= (:x + :width)")
                .add("(y + height) <= (:y + :height)");
            parameters
                .addValue("boundaries", toPolygon(regionToSearch))
                .addValue("x", regionToSearch.getX())
                .addValue("y", regionToSearch.getY())
                .addValue("width", regionToSearch.getWidth())
                .addValue("height", regionToSearch.getHeight());
        }
        if (zIndexCursor != null) {
            clauses.add("z_index > :cursor");
            parameters.addValue("cursor", zIndexCursor);
        }

        return jdbcTemplate.query(
            "SELECT widget_id, x, y, width, height, z_index, modified_at " +
                "FROM widget " +
                "WHERE " + clauses + " " +
                "ORDER BY z_index " +
                "LIMIT :limit",
            parameters,
            ROW_MAPPER
        );
    }

    private static String toPolygon(Region region) {
        String lowerLeftCorner = String.format("%d %d", region.getX(), region.getY());
        String lowerRightCorner = String.format("%d %d", region.getX() + region.getWidth(), region.getY());
        String higherRightCorner = String.format("%d %d",
            region.getX() + region.getWidth(), region.getY() + region.getHeight());
        String higherLeftCorner = String.format("%d %d", region.getX(), region.getY() + region.getHeight());
        return String.format("POLYGON ((%s, %s, %s, %s, %s))",
            lowerLeftCorner, lowerRightCorner, higherRightCorner, higherLeftCorner, lowerLeftCorner);
    }
}
