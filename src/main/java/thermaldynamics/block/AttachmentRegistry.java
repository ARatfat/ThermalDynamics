package thermaldynamics.block;

import thermaldynamics.ducts.attachments.facades.Cover;
import thermaldynamics.ducts.attachments.filter.FilterItem;
import thermaldynamics.ducts.attachments.servo.ServoFluid;
import thermaldynamics.ducts.attachments.servo.ServoItem;

import java.util.HashMap;

public class AttachmentRegistry {
    public static HashMap<String, Class<? extends Attachment>> nameToAttachment = new HashMap<String, Class<? extends Attachment>>();
    public static HashMap<Class<? extends Attachment>, String> attachmentToName = new HashMap<Class<? extends Attachment>, String>();
    public final static byte FACADE = 0;
    public final static byte SERVO_FLUID = 1;
    public final static byte SERVO_INV = 2;
    public final static byte FILTER_FLUID = 3;
    public final static byte FILTER_INV = 4;


    public static Attachment createAttachment(TileMultiBlock tile, byte side, int id) {
        if (id == FACADE) {
            return new Cover(tile, side);
        } else if (id == SERVO_FLUID) {
            return new ServoFluid(tile, side);
        } else if (id == SERVO_INV) {
            return new ServoItem(tile, side);
        } else if (id == FILTER_INV) {
            return new FilterItem(tile, side);
        }

        throw new RuntimeException("Illegal Attachment ID");
    }
}
