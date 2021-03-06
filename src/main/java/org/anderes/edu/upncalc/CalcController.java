package org.anderes.edu.upncalc;

import static javafx.event.ActionEvent.ACTION;
import static javafx.scene.input.KeyCode.ADD;
import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.DIGIT1;
import static javafx.scene.input.KeyCode.DIGIT3;
import static javafx.scene.input.KeyCode.DIGIT7;
import static javafx.scene.input.KeyCode.DIVIDE;
import static javafx.scene.input.KeyCode.ENTER;
import static javafx.scene.input.KeyCode.ESCAPE;
import static javafx.scene.input.KeyCode.MINUS;
import static javafx.scene.input.KeyCode.MULTIPLY;
import static javafx.scene.input.KeyCode.SHIFT;
import static javafx.scene.input.KeyCode.SUBTRACT;
import static javafx.scene.input.KeyEvent.KEY_PRESSED;
import static javafx.scene.input.KeyEvent.KEY_RELEASED;
import static javafx.scene.input.KeyEvent.KEY_TYPED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.math.NumberUtils.INTEGER_ZERO;
import static org.apache.commons.lang3.math.NumberUtils.createBigDecimal;

import java.math.BigDecimal;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactfx.BiEventStream;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class CalcController implements Initializable {

    @FXML
    private Button btnZero;
    @FXML
    private Button btnOne;
    @FXML
    private Button btnTwo;
    @FXML
    private Button btnThree;
    @FXML
    private Button btnFour;
    @FXML
    private Button btnFive;
    @FXML
    private Button btnSix;
    @FXML
    private Button btnSeven;
    @FXML
    private Button btnEight;
    @FXML
    private Button btnNine;
    @FXML
    private Button btnPoint;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnCE;
    @FXML
    private Button btnStack;
    @FXML
    private Button btnAddition;
    @FXML
    private Button btnSubtract;
    @FXML
    private Button btnMultiply;
    @FXML
    private Button btnDivide;
    @FXML
    private Button btnSigned;
    @FXML
    private TextField inValue;
    @FXML
    private Button btnEnter;
    @FXML
    private Button btnUndo;
    @FXML
    private Button btnInverse;
    @FXML
    private Button btnHelp;
    @FXML
    private Button btnSquaredRoot;
    @FXML
    private Button btnPi;
    @FXML
    private Button btnSquared;
    @FXML
    private ListView<BigDecimal> lwStack;
    
    @Inject
    private Calc calc;
    @Inject
    private HelpDialog helpDialog;
    
    private final static String ZERO = INTEGER_ZERO.toString();
    private final ObservableList<BigDecimal> stack = FXCollections.observableArrayList();
    private final Logger logger = LogManager.getLogger(this.getClass().getName());

    private final UnaryOperator<Change> textFormatterDigitFilter = change -> {
        final String text = change.getText();
        if (text.matches("[0-9.-]*")) {
            return change;
        }
        return null;
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.debug("CalcController-Instanz: " + this.toString());
        initUiControls();
        initToolTips(resources);
               
        /* ReactFx siehe https://github.com/TomasMikula/ReactFX */
        /*                                                      */
        EventStreams.eventsOf(btnEnter, ACTION).subscribe(event -> addNewValueIfNotEmpty());
        EventStreams.eventsOf(btnAddition, ACTION).subscribe(event -> addition());
        EventStreams.eventsOf(btnSubtract, ACTION).subscribe(event -> subtract());
        EventStreams.eventsOf(btnMultiply, ACTION).subscribe(event -> multiply());
        EventStreams.eventsOf(btnDivide, ACTION).subscribe(event -> divide(), exception -> handleError(exception, resources));
        EventStreams.eventsOf(btnSquared, ACTION).subscribe(event -> squared());
        EventStreams.eventsOf(btnSquaredRoot, ACTION).subscribe(event -> squaredRoot(), exception -> handleError(exception, resources));
        EventStreams.eventsOf(btnPi, ACTION).subscribe(event -> pi());
        EventStreams.eventsOf(btnCancel, ACTION).subscribe(event -> cancelStack());
        EventStreams.eventsOf(btnCE, ACTION).subscribe(event -> removeFromStack());
        EventStreams.eventsOf(btnSigned, ACTION).subscribe(event -> signedInput());
        EventStreams.eventsOf(btnStack, ACTION).subscribe(event -> formStackToInput());
        EventStreams.eventsOf(btnUndo, ACTION).subscribe(event -> undoLastFunction());
        EventStreams.eventsOf(btnInverse, ACTION).subscribe(event -> inverse());
        EventStreams.eventsOf(btnHelp, ACTION).subscribe(event -> helpDialog.showAndWait());
        
        EventStream<KeyEvent> keyPressed = EventStreams.eventsOf(inValue, KEY_PRESSED);
        EventStream<KeyEvent> keyReleased = EventStreams.eventsOf(inValue, KEY_RELEASED);
        EventStream<KeyEvent> keyTyped = EventStreams.eventsOf(inValue, KEY_TYPED);
        EventStream<KeyEvent> keyPressedOrReleased = EventStreams.merge(keyPressed, keyReleased);
        EventStream<Boolean> shiftPresses = keyPressedOrReleased.filter(key -> key.getCode().equals(SHIFT)).map(key -> key.isShiftDown());
        
        keyTyped.filter(key -> notAllowedInputCharacter(key.getCharacter())).subscribe(key -> key.consume());
        
        final EventStream<KeyCode> keyCode = keyReleased.map(key -> key.getCode());
        keyCode.filter(key -> key.equals(ENTER)).subscribe(key -> btnEnter.fire());
        keyCode.filter(key -> key.equals(ESCAPE)).subscribe(key -> initValueForInputField());
        keyCode.filter(key -> key.equals(ADD)).subscribe(key -> btnAddition.fire());
        keyCode.filter(key -> key.equals(SUBTRACT)).subscribe(key -> btnSubtract.fire());
        keyCode.filter(key -> key.equals(MINUS)).subscribe(key -> btnSubtract.fire());
        keyCode.filter(key -> key.equals( MULTIPLY)).subscribe(key -> btnMultiply.fire());
        keyCode.filter(key -> key.equals(DIVIDE)).subscribe(key -> btnDivide.fire());
        keyCode.filter(key -> key.equals(DELETE)).subscribe(key -> initValueForInputField());
        keyCode.filter(key -> key.equals(BACK_SPACE)).filter(key -> isEmpty(inValue.getText())).subscribe(key -> initValueForInputField());
        keyCode.filter(key -> key.isDigitKey()).filter(key -> startsWith(inValue.getText(), ZERO)).subscribe(key -> removeLeadingZero());
        
        final BiEventStream<KeyCode, Boolean> combo = EventStreams.combine(keyCode, shiftPresses);
        combo.subscribe((key, shift) -> {
            if (shift && key.equals(DIGIT1)) {
                btnAddition.fire();
            }
            if (shift && key.equals(DIGIT3)) {
                btnMultiply.fire();
            }
            if (shift && key.equals(DIGIT7)) {
                btnDivide.fire();
            }
        });
        
        Platform.runLater(() -> {
            final EventStream<KeyEvent> sceneKeyReleased = EventStreams.eventsOf(inValue.getScene(), KEY_RELEASED)
                            .hook(event -> logger.trace("Taste: " + event.getCode()))
                            .filter(event -> event.getCode().isDigitKey());
            sceneKeyReleased.subscribe(event -> redirect(event));
        });
        EventStreams.eventsOf(lwStack, MOUSE_CLICKED)
            .filter(event -> event.getClickCount() == 2)
            .map(event -> lwStack.getSelectionModel().getSelectedItem())
            .filter(n -> n != null)
            .subscribe(n -> inValue.setText(n.toString()));
    }

    private void pi() {
        inValue.appendText(calc.pi().toString());
    }

    private void initToolTips(ResourceBundle resources) {
        final Tooltip toolTipForBtnStack = new Tooltip(resources.getString("command.st"));
        btnStack.setTooltip(toolTipForBtnStack);
        final Tooltip toolTipForBtnC = new Tooltip(resources.getString("command.c"));
        btnCancel.setTooltip(toolTipForBtnC);
        final Tooltip toolTipForBtnCE = new Tooltip(resources.getString("command.ce"));
        btnCE.setTooltip(toolTipForBtnCE);
        final Tooltip toolTipForBtnUndo = new Tooltip(resources.getString("command.undo"));
        btnUndo.setTooltip(toolTipForBtnUndo);
    }

    private void initUiControls() {
        final TextFormatter<String> textFormatter = new TextFormatter<>(textFormatterDigitFilter);
        inValue.setTextFormatter(textFormatter);
        lwStack.setItems(stack);
        lwStack.setFocusTraversable(false);
        
        btnOne.setOnAction(e -> appendText("1"));
        btnTwo.setOnAction(e -> appendText("2"));
        btnThree.setOnAction(e -> appendText("3"));
        btnFour.setOnAction(e -> appendText("4"));
        btnFive.setOnAction(e -> appendText("5"));
        btnSix.setOnAction(e -> appendText("6"));
        btnSeven.setOnAction(e -> appendText("7"));
        btnEight.setOnAction(e -> appendText("8"));
        btnNine.setOnAction(e -> appendText("9"));
        btnZero.setOnAction(e -> appendText("0"));
        btnPoint.setOnAction(e -> appendText("."));
    }

    private boolean notAllowedInputCharacter(final String character) {
        final String text = inValue.getText();
        if (character.matches("[0-9.]")) {
            if (text.contains(".") && character.matches("[.]")) {
                return true;
            } else if (text.length() == 0 && character.matches("[.]")) {
                return true;
            }
        } else {
            return true;
        }
        return false;
    }

    private void formStackToInput() {
        calc.removeFromStack().ifPresent(d -> inValue.setText(d.toString()));
        stack.setAll(calc.getStack());
        inValue.requestFocus();
        inValue.end();
    }

    private void signedInput() {
        if (isNotEmpty(inValue.getText())) {
            final BigDecimal temp = new BigDecimal(inValue.getText());
            inValue.setText(temp.negate().toString());
        }
    }

    private void undoLastFunction() {
        calc.undo();
        stack.setAll(calc.getStack());
    }

    private void squared() {
        addNewValueIfNotEmpty();
        final Optional<BigDecimal> calcValue = calc.squared();
        handleCalcValue(calcValue);
    }

    private void squaredRoot() {
        addNewValueIfNotEmpty();
        try {
            final Optional<BigDecimal> calcValue = calc.squaredRoot();
            handleCalcValue(calcValue);
        } catch (NumberFormatException e) {
            stack.setAll(calc.getStack());
            throw new IllegalStateException(e);
        }
    }

    private void inverse() {
        addNewValueIfNotEmpty();
        final Optional<BigDecimal> calcValue = calc.inverse();
        handleCalcValue(calcValue);
    }
    
    private void removeFromStack() {
        calc.removeFromStack();
        stack.setAll(calc.getStack());
    }

    private void divide() {
        addNewValueIfNotEmpty();
        try {
            final Optional<BigDecimal> calcValue = calc.divide();
            handleCalcValue(calcValue);
        } catch (ArithmeticException e) {
            stack.setAll(calc.getStack());
            throw new IllegalStateException(e);
        }
    }

    private void multiply() {
        addNewValueIfNotEmpty();
        final Optional<BigDecimal> calcValue = calc.multiply();
        handleCalcValue(calcValue);
    }

    private void addition() {
        addNewValueIfNotEmpty();
        final Optional<BigDecimal> calcValue = calc.addition();
        handleCalcValue(calcValue);
    }
    
    private void subtract() {
        addNewValueIfNotEmpty();
        final Optional<BigDecimal> calcValue = calc.subtract();
        handleCalcValue(calcValue);
    }

    private void handleCalcValue(final Optional<BigDecimal> calcValue) {
        stack.setAll(calc.getStack());
        if (calcValue.isPresent()) {
            lwStack.scrollTo(calcValue.get());
        } else {
            throw new IllegalStateException();
        }
    }

    private void addNewValueIfNotEmpty() {
        if (isEmpty(inValue.getText())) {
            return;
        }
        final BigDecimal newValue = createBigDecimal(inValue.getText());
        newValueToStack(newValue);
    }
    
    private void appendText(final String text) {
        if (notAllowedInputCharacter(text)) {
            return;
        }
        inValue.appendText(text);
        if (inValue.getText().startsWith(ZERO)) {
            removeLeadingZero();
        }
    }

    private void cancelStack() {
        while(calc.removeFromStack().isPresent()){};
        stack.clear();
    }

    private void handleError(final Throwable e, final ResourceBundle resources) {
        logger.warn(e.getMessage());
        final String message = resources.getString("error.wrong.input");
        Alert alert = new Alert(AlertType.ERROR);
        alert.setContentText(message);
        alert.show();
    }

    private void redirect(KeyEvent event) {
        if (!inValue.isFocused()) {
            inValue.requestFocus();
            appendText(event.getText());
        }
    }
    
    private void initValueForInputField() {
        inValue.clear();
        inValue.setText(EMPTY);
        inValue.end();
    }
    
    private void removeLeadingZero() {
        if (inValue.getText().matches("[0][.]?[0-9]*")) {
            return;
        }
        inValue.setText(removeStart(inValue.getText(), ZERO));
        inValue.end();
    }
    
    private void newValueToStack(final BigDecimal newValue) {
        initValueForInputField();
        calc.addToStack(newValue);
        stack.setAll(calc.getStack());
        lwStack.scrollTo(newValue);
    }

 }
