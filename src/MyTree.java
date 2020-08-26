import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class MyTree {
   public static void main(String[] args) {
      Display myDisplay = new Display();
      Shell myShell = new Shell(myDisplay);
      myShell.setText("My Tree");
      myShell.setBounds(120, 120, 220, 220);
      myShell.setLayout(new FillLayout());
      final Tree tree = new Tree(myShell, SWT.SINGLE);
      for (int i = 1; i < 4; i++) {
         TreeItem parent1 = new TreeItem(tree, 0);
         parent1.setText("Paren1 - " + i);
         for (int j = 1; j < 4; j++) {
            TreeItem parent2 = new TreeItem(parent1,0);
            parent2.setText("Parent2 - " + j);
            for (int k = 1; k < 4; k++) {
               TreeItem child = new TreeItem(parent2, 0);
               child.setText("Child - " + k);
            }
         }
      }
      myShell.open();
      while (!myShell.isDisposed()) {
         if (!myDisplay.readAndDispatch()) myDisplay.sleep();
      }
      myDisplay.dispose();
   }
}