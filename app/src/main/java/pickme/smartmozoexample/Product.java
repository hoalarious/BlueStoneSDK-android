package pickme.smartmozoexample;

import java.io.Serializable;

/**
 * Created by Hoa on 9/01/2016.
 */
public class Product implements Serializable {
    public String name;
    public String beacon;
    public String code;
    public int price;
    public String image;
    public String colours;
    public String sizes;
    public String comments;
    public byte counter;
    public byte ingoreCounter;
}