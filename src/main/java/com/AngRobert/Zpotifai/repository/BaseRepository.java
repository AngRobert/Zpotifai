package com.AngRobert.Zpotifai.repository;

import com.AngRobert.Zpotifai.util.DBConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// this class tries to eliminate code redundancy, especially jdbc sql code.
public abstract class BaseRepository<T> {

    protected abstract T mapRow(ResultSet rs) throws SQLException;
    protected abstract String getTableName();
    protected abstract String getIdColumnName();

    protected String getNameColumnName() {
        return "name";
    }

    // for tables with children, for example Host and Artist are children of Creator
    protected String getBaseTableName() {
        return getTableName();
    }

    public int getIdByName(String name) {
        String sql = "SELECT " + getIdColumnName() + " FROM " + getBaseTableName() + " WHERE LOWER(" + getNameColumnName() + ") = LOWER(?)";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error finding ID by name: " + e.getMessage());
        }
        return -1;
    }

    // used for streaming simulation
    public T findById(int id) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE " + getBaseTableName() + "." + getIdColumnName() + " = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return mapRow(rs);
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return null;
    }

    public Boolean checkDuplicates(String name) {
        String sql = "SELECT 1 FROM " + getBaseTableName() + " WHERE LOWER(" + getNameColumnName() + ") = LOWER(?)";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
        catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }

        return false;
    }

    // used for search feature
    public List<T> searchByColumnName(String columnName, String value) {
        String sql = "SELECT * FROM " + getTableName() + " WHERE LOWER(" + columnName + ") LIKE LOWER(?)";
        List<T> rez = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setString(1, value + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rez.add(mapRow(rs));
            }
        }
        catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return rez;
    }

    // used TagRepository when listing all tags
    public List<String> findAll() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT * FROM " + getTableName();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(rs.getString("description"));
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
        return list;
    }

    // main add function
    public int add(String tableName, List<String> columns, List<Object> values) {
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("The number of values inputted must match the number of columns!");
        }
        // builds the sql query
        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(tableName)
                .append(" (")
                .append(String.join(", ", columns))
                .append(") VALUES (");

        // continues building the sql query by adding the "?" parameters
        for (int i = 0; i < values.size(); i++) {
            sql.append("?");
            if (i < values.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        // RETURN_GENERATED_KEYS ne spune ca id-ul generat random poate fi "retrieved"
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                // returneaza id-ul generat pentru a fi folosit in subclase
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error inserting into " + tableName + ": " + e.getMessage());
        }
        return -1;
    }

    public int add(List<String> columns, List<Object> values) {
        return add(getBaseTableName(), columns, values);
    }

    private record ColumnSplit(List<String> parentCols, List<Object> parentVals,

                               List<String> childCols, List<Object> childVals) {}

    // takes all the columns that need to be inserted into a table and splits them into ones that need to be inserted into the parent table
    // and the child table
    private ColumnSplit splitColumns(List<String> childOnlyColumns, List<String> columns, List<Object> values) {
        List<String> parentCols = new ArrayList<>();
        List<Object> parentVals = new ArrayList<>();
        List<String> childCols = new ArrayList<>();
        List<Object> childVals = new ArrayList<>();

        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i);
            boolean isChild = false;
            for (String childCol : childOnlyColumns) {
                if (childCol.equalsIgnoreCase(col)) {
                    isChild = true;
                    break;
                }
            }

            if (isChild) {
                childCols.add(col);
                childVals.add(values.get(i));
            } else {
                parentCols.add(col);
                parentVals.add(values.get(i));
            }
        }
        return new ColumnSplit(parentCols, parentVals, childCols, childVals);
    }

    protected int addWithChild(String childTableName, List<String> childOnlyColumns, List<String> columns, List<Object> values) {
        ColumnSplit split = splitColumns(childOnlyColumns, columns, values);
        // first inserts into the parent table
        int id = add(getBaseTableName(), split.parentCols(), split.parentVals());
        if (id != -1) {
            List<String> cCols = new ArrayList<>(split.childCols());
            List<Object> cVals = new ArrayList<>(split.childVals());
            cCols.add(getIdColumnName());
            cVals.add(id);
            add(childTableName, cCols, cVals);
        }
        return id;
    }

    // update logic is similar with add logic
    public int update(String tableName, int id, List<String> columns, List<Object> values) {
        if (columns.isEmpty()) return -1;
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException("The number of values inputted must match the number of columns!");
        }

        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(tableName)
                .append(" SET ");

        for (int i = 0; i < columns.size(); i++) {
            sql.append(columns.get(i)).append(" = ?");
            if (i < columns.size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE ").append(getIdColumnName()).append(" = ?");

        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql.toString())) {
            for (int i = 0; i < values.size(); i++) {
                stmt.setObject(i + 1, values.get(i));
            }
            stmt.setInt(values.size() + 1, id);
            stmt.executeUpdate();
            return id;
        } catch (SQLException e) {
            System.out.println("Error updating " + tableName + ": " + e.getMessage());
        }
        return -1;
    }

    public int update(int id, List<String> columns, List<Object> values) {
        return update(getBaseTableName(), id, columns, values);
    }

    protected int updateWithChild(String childTableName, List<String> childColumns, int id, List<String> columns, List<Object> values) {
        ColumnSplit split = splitColumns(childColumns, columns, values);
        update(getBaseTableName(), id, split.parentCols(), split.parentVals());
        update(childTableName, id, split.childCols(), split.childVals());
        return id;
    }

    public void deleteById(int id) {
        String sql = "DELETE FROM " + getBaseTableName() + " WHERE " + getIdColumnName() + " = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    public void incrementStreams(int id) {
        String sql = "UPDATE " + getBaseTableName() + " SET streams = streams + 1 WHERE " + getIdColumnName() + " = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error incrementing streams: " + e.getMessage());
        }
    }

    public String getSearchDetails(int id, List<String> columnNames) {
        String columns = String.join(", ", columnNames);
        String sql = "SELECT " + columns + " FROM " + getTableName() + " WHERE " + getBaseTableName() + "." + getIdColumnName() + " = ?";
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                StringBuilder details = new StringBuilder("\nDetails:\n");
                for (String col : columnNames) {

                    String displayName = col.substring(0, 1).toUpperCase() + col.substring(1).replace("_", " ");
                    Object value = rs.getObject(col);

                    // format for song length (seconds to MM:SS)
                    if (col.equalsIgnoreCase("length") && value instanceof Number) {
                        int totalSeconds = ((Number) value).intValue();
                        int mins = totalSeconds / 60;
                        int secs = totalSeconds % 60;
                        value = String.format("%d:%02d", mins, secs);
                    }

                    details.append(displayName).append(": ").append(value).append("\n");
                }
                return details.toString();
            }
        } catch (SQLException e) {
            System.out.println("Error fetching details: " + e.getMessage());
        }
        return "Item not found.";
    }

    protected List<String> getRelatedNames(String sql, int id) {
        List<String> names = new ArrayList<>();
        try (PreparedStatement stmt = DBConnection.get().prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        } catch (SQLException e) {
            System.out.println("Error fetching related names: " + e.getMessage());
        }
        return names;
    }
}
