package aqua.blatt1.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToggleController implements ActionListener {
    private TankModel tankModel;
    private String fishID;

    public ToggleController(TankModel tankModel, String fishID) {
        this.tankModel = tankModel;
        this.fishID = fishID;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        tankModel.locateFishGlobally(fishID);
    }
}
