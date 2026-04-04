/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.group5.paul_esys.ui;

import com.formdev.flatlaf.ui.FlatRoundBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author nytri
 */
public class PanelRoundBorder extends CompoundBorder {

  public PanelRoundBorder() {
    super(new FlatRoundBorder(), new EmptyBorder(2, 2, 2, 2));
  }
}
