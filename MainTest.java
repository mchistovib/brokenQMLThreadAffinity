import io.qt.QtPropertyConstant;
import io.qt.QtPropertyNotify;
import io.qt.QtPropertyReader;
import io.qt.QtUtilities;
import io.qt.core.*;
import io.qt.qml.QQmlApplicationEngine;
import io.qt.widgets.QApplication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainTest {

    // test commit
    public static void main(String[] args) {
        ///////////////////////////////////////////////////////////
        ///////////////////////// for QT Quick ///////////////////
        QtUtilities.initializePackage("io.qt.quick");
        QtUtilities.loadQtLibrary("QuickControls2");
        //////////////////////////////////////////////////////////
        QApplication.initialize(args);
        QQmlApplicationEngine engine = new QQmlApplicationEngine();

        engine.rootContext().setContextProperty("controller", new Controller());

        engine.objectCreated.connect((obj, url) -> {
            if (obj == null) {
                System.out.println("Failed to load main qml");
                QApplication.exit(-1);
            } else {
                System.out.println("Qml loaded");
            }
        }, Qt.ConnectionType.QueuedConnection);
        engine.load(new QUrl("qrc:/mainTest.qml"));

        QApplication.exec();
        QApplication.shutdown();
    }

    public static class Controller extends QObject {
        private final TestModel m_model;

        public Controller() {
            m_model = new TestModel();
            m_model.addRows(Arrays.asList(new QPair<>(12, "test one"), new QPair<>(12, "test one"),
                    new QPair<>(33, "test two"), new QPair<>(79, "test three"), new QPair<>(45, "test four") ));
            m_model.addRows(Arrays.asList(new QPair<>(12, "test one"), new QPair<>(12, "test one"),
                    new QPair<>(33, "test two"), new QPair<>(79, "test three"), new QPair<>(45, "test four") ));
        }

        @QtPropertyNotify(name = "tableModel")
        private final Signal0 tableModelChanged = new Signal0();

        @QtPropertyReader(name = "tableModel") @QtPropertyConstant
        public TestModel getModel() {
            return m_model;
        }

        public void addOtherThread() {
            QThread test = new QThread() {
                @Override protected void run() {
                    m_model.addRows(Arrays.asList(new QPair<>(12, "other thread one"), new QPair<>(12, "other thread two"))); // !!! Creates java core crash, no any exceptions about thread affinity from jambi
                }
            }; 
            test.start();
        }
    }

    public static class TestModel extends QAbstractTableModel {
        private final List<QPair<Integer, String>> rows = new ArrayList<>();

        public void addRows(List<QPair<Integer, String>> newRows) {
            beginInsertRows(null, rows.size(), rows.size() + newRows.size());
            rows.addAll(newRows);
            endInsertRows();
        }

        public void removeSomeRows(int from, int to) {
            beginRemoveRows(null, from, to);
            int count = 0;
            while (count < to - from) {
                rows.remove(from);
                count++;
            }
            endRemoveRows();
        }

        public void removeAllRows() {
            removeSomeRows(0, rows.size() - 1);
        }

        @Override public int columnCount(QModelIndex index) {
            return 2;
        }

        @Override public Object headerData(int section, Qt.Orientation orientation, int role) {
            return section == 0 ? "Some num" : "Some string";
        }

        @Override public Object data(QModelIndex index, int role) {
            if (index.row() < 0 || index.row() >= rows.size()) {
                return null;
            }
            if (index.column() >= 2) {
                return null;
            }
            if (role == Qt.ItemDataRole.DisplayRole) {
                return index.column() == 0 ? String.valueOf(rows.get(index.row()).first) : rows.get(index.row()).second;
            }
            return null;
        }

        @Override public int rowCount(QModelIndex index) {
            return Math.max(rows.size(), 10);
        }
    }
}
