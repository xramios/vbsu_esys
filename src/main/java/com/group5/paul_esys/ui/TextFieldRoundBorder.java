/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.group5.paul_esys.ui;

import com.formdev.flatlaf.ui.FlatRoundBorder;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author nytri
 */
public class TextFieldRoundBorder extends CompoundBorder {

  public TextFieldRoundBorder() {
    super(new FlatRoundBorder(), new EmptyBorder(2, 2, 2, 2));
  }
}
