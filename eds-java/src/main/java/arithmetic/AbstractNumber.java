/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package arithmetic;

/**
 *
 * @author bogachenko
 */

public interface AbstractNumber {
    AbstractNumber add(AbstractNumber b);
    AbstractNumber sub(AbstractNumber b);
    AbstractNumber mult(AbstractNumber b);
    AbstractNumber div(AbstractNumber b);
}
