/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.examplehelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeItem.TreeModificationEvent;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableColumn.CellDataFeatures;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.ChoiceBoxTreeTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.opentravel.schemacompiler.codegen.example.ExampleBuilder;
import org.opentravel.schemacompiler.codegen.example.ExampleDocumentBuilder;
import org.opentravel.schemacompiler.codegen.example.ExampleGeneratorOptions;
import org.opentravel.schemacompiler.codegen.example.ExampleJsonBuilder;
import org.opentravel.schemacompiler.ioc.CompilerExtensionRegistry;
import org.opentravel.schemacompiler.loader.LibraryInputSource;
import org.opentravel.schemacompiler.loader.LibraryLoaderException;
import org.opentravel.schemacompiler.loader.LibraryModelLoader;
import org.opentravel.schemacompiler.loader.impl.LibraryStreamInputSource;
import org.opentravel.schemacompiler.model.NamedEntity;
import org.opentravel.schemacompiler.model.TLActionFacet;
import org.opentravel.schemacompiler.model.TLBusinessObject;
import org.opentravel.schemacompiler.model.TLChoiceObject;
import org.opentravel.schemacompiler.model.TLCoreObject;
import org.opentravel.schemacompiler.model.TLLibrary;
import org.opentravel.schemacompiler.model.TLModel;
import org.opentravel.schemacompiler.model.TLOperation;
import org.opentravel.schemacompiler.model.TLReferenceType;
import org.opentravel.schemacompiler.model.TLResource;
import org.opentravel.schemacompiler.repository.ProjectManager;
import org.opentravel.schemacompiler.validate.FindingMessageFormat;
import org.opentravel.schemacompiler.validate.FindingType;
import org.opentravel.schemacompiler.validate.ValidationFindings;
import org.opentravel.schemacompiler.xml.XMLPrettyPrinter;
import org.w3c.dom.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * JavaFX controller class for the OTA2 Example Helper application.
 */
public class ExampleHelperController {
	
	public static final String FXML_FILE = "/ota2-example-helper.fxml";
	
	private Stage primaryStage;
	
	@FXML private TextField libraryText;
	@FXML private ChoiceBox<String> bindingStyleChoice;
	@FXML private ChoiceBox<OTMObjectChoice> entityChoice;
	@FXML private Spinner<Integer> repeatCountSpinner;
	@FXML private ToggleGroup formatGroup;
	@FXML private RadioButton xmlRadio;
	@FXML private RadioButton jsonRadio;
	@FXML private TreeTableView<EntityMemberNode> treeView;
	@FXML private TreeTableColumn<EntityMemberNode,String> objectPropertyColumn;
	@FXML private TreeTableColumn<EntityMemberNode,String> facetSelectionColumn;
	@FXML private Button saveButton;
	@FXML private Label statusBarLabel;
	@FXML private VBox previewVBox;
	private VirtualizedScrollPane<?> previewScrollPane;
	private CodeArea previewPane;
	
	private File modelFile;
	private File exampleFolder;
	private TLModel model;
	private NamedEntity selectedObject;
	private NamedEntity oldSelectedObject;
	private FacetSelections facetSelections;
	
	/**
	 * Constructs a new file chooser instance using the information provided.
	 * 
	 * @param title  the title of the file chooser dialog
	 * @param initialDirectory  the initial directory for the chooser
	 * @param extensionFilters  the extension filters to include in the chooser
	 * @return FileChooser
	 */
	private FileChooser newFileChooser(String title, File initialDirectory, FileChooser.ExtensionFilter... extensionFilters) {
		FileChooser chooser = new FileChooser();
		
		chooser.setTitle( title );
		chooser.setInitialDirectory( initialDirectory );
		chooser.getExtensionFilters().addAll( extensionFilters );
		return chooser;
	}
	
	/**
	 * Called when the user clicks the button to load a new project or library file.
	 * 
	 * @param event  the action event that triggered this method call
	 */
	@FXML public void selectLibrary(ActionEvent event) {
		File initialDirectory = (modelFile != null) ?
				modelFile.getParentFile() : UserSettings.load().getLastModelFile().getParentFile();
		FileChooser chooser = newFileChooser( "Select OTM Library or Project", initialDirectory,
				new FileChooser.ExtensionFilter( "OTM Projects", "*.otp" ),
				new FileChooser.ExtensionFilter( "OTM Libraries", "*.otm" ),
				new FileChooser.ExtensionFilter( "All Files", "*.*" ) );
		File selectedFile = chooser.showOpenDialog( primaryStage );
		
		if ((selectedFile != null) && selectedFile.exists()) {
			Runnable r = new BackgroundTask( "Loading Library: " + selectedFile.getName() ) {
				public void execute() throws Throwable {
					try {
						ValidationFindings findings;
						TLModel newModel = null;
						
						if (selectedFile.getName().endsWith(".otp")) {
							ProjectManager manager = new ProjectManager( false );
							
							findings = new ValidationFindings();
							manager.loadProject( selectedFile, findings );
							
							newModel = manager.getModel();
							
						} else { // assume OTM library file
					        LibraryInputSource<InputStream> libraryInput = new LibraryStreamInputSource( selectedFile );
					        LibraryModelLoader<InputStream> modelLoader = new LibraryModelLoader<InputStream>();
					        
					        findings = modelLoader.loadLibraryModel( libraryInput );
							newModel = modelLoader.getLibraryModel();
						}
						
						if ((findings == null) || !findings.hasFinding( FindingType.ERROR )) {
							model = newModel;
							modelFile = selectedFile;
							updateEntityChoices();
							
						} else {
							System.out.println(selectedFile.getName() + " - Error/Warning Messages:");

							for (String message : findings.getAllValidationMessages(FindingMessageFormat.IDENTIFIED_FORMAT)) {
								System.out.println("  " + message);
							}
							throw new LibraryLoaderException("Validation errors detected in model (see log for details)");
						}
						
					} finally {
						updateControlStates();
					}
				}
			};
			
			new Thread( r ).start();
		}
	}
	
	/**
	 * Called when the user changes the default binding style.
	 */
	private void handleBindingStyleChange() {
		String selectedStyle = (String) bindingStyleChoice.getValue();
		String currentStyle = CompilerExtensionRegistry.getActiveExtension();

		if ((selectedStyle != null) && !selectedStyle.equals(currentStyle)) {
			CompilerExtensionRegistry.setActiveExtension(selectedStyle);
			refreshExample();
		}
	}
	
	private void updateEntityChoices() {
		List<OTMObjectChoice> selectableObjects = new ArrayList<>();

		// Collect the selectable objects for the combo-box
		if (model != null) {
			for (TLLibrary library : model.getUserDefinedLibraries()) {
				for (TLBusinessObject bo : library.getBusinessObjectTypes()) {
					selectableObjects.add( new OTMObjectChoice( bo ) );
				}
				for (TLCoreObject core : library.getCoreObjectTypes()) {
					selectableObjects.add( new OTMObjectChoice( core ) );
				}
				for (TLChoiceObject choice : library.getChoiceObjectTypes()) {
					selectableObjects.add( new OTMObjectChoice( choice ) );
				}
				for (TLResource resource : library.getResourceTypes()) {
					for (TLActionFacet actionFacet : resource.getActionFacets()) {
						if (actionFacet.getReferenceType() != TLReferenceType.NONE) {
							selectableObjects.add( new OTMObjectChoice( actionFacet ) );
						}
					}
				}
				if (library.getService() != null) {
					for (TLOperation op : library.getService().getOperations()) {
						selectableObjects.add( new OTMObjectChoice( op ) );
					}
				}
			}
			
			// Sort the objects in alphabetical order according to their display label
			Collections.sort( selectableObjects, new Comparator<OTMObjectChoice>() {
				public int compare(OTMObjectChoice item1, OTMObjectChoice item2) {
					return item1.toString().compareTo( item2.toString() );
				}
			});
		}
		
		Platform.runLater( new Runnable() {
			public void run() {
				entityChoice.setItems( FXCollections.observableArrayList( selectableObjects ) );
				
				if (selectableObjects.size() > 0) {
					entityChoice.setValue( selectableObjects.get( 0 ) );
				}
			}
		} );
	}
	
	/**
	 * Called when the entity selection has been modified by the user.
	 */
	private void entitySelectionChanged() {
		OTMObjectChoice selection = entityChoice.getValue();
		
		if (selection != null) {
			EntityMemberTreeBuilder treeBuilder = new EntityMemberTreeBuilder( selection.getOtmObject() );
			EntityMemberNode rootNode = treeBuilder.buildTree();
			
			treeView.setRoot( new EntityMemberTreeItem( rootNode ) );
			facetSelections = treeBuilder.getFacetSelections();
			selectedObject = selection.getOtmObject();
			
			facetSelections.setListener( new FacetSelectionListener() {
				public void facetSelectionChanged(EntityFacetSelection modifiedSelection) {
					refreshExample();
				}
			});
			
		} else {
			if (facetSelections != null) {
				facetSelections.setListener( null );
			}
			treeView.setRoot( null );
			facetSelections = null;
			selectedObject = null;
		}
		refreshExample();
	}
	
	/**
	 * Refreshes the contents of the example text viewer.
	 */
	private void refreshExample() {
		Platform.runLater( new Runnable() {
			public void run() {
				if ((model != null) && (selectedObject != null)) {
					try {
						double yScroll = (selectedObject != oldSelectedObject) ? 0.0 : previewScrollPane.estimatedScrollYProperty().getValue();
						ExampleGeneratorOptions options = new ExampleGeneratorOptions();
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						SyntaxHighlightBuilder highlightingBuilder;
						
						facetSelections.configureExampleOptions( options );
						options.setMaxRepeat( repeatCountSpinner.getValue() );
						
						if (xmlRadio.isSelected()) {
							ExampleBuilder<Document> builder = new ExampleDocumentBuilder( options ).setModelElement( selectedObject );
							Document domDocument = builder.buildTree();
							
							highlightingBuilder = new XmlHighlightBuilder();
							new XMLPrettyPrinter().formatDocument( domDocument, out );
							
						} else { // json selected
							ExampleJsonBuilder exampleBuilder = new ExampleJsonBuilder( options );
							ObjectMapper mapper = new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT );
							JsonNode node;
							
							highlightingBuilder = new JsonHighlightBuilder();
							exampleBuilder.setModelElement( selectedObject );
							node = exampleBuilder.buildTree();
							mapper.writeValue( out, node );
						}
						previewPane.replaceText( new String( out.toByteArray(), "UTF-8" ) );
						previewPane.setStyleSpans( 0, highlightingBuilder.computeHighlighting( previewPane.getText() ) );
						Platform.runLater( new Runnable() {
							public void run() {
								previewScrollPane.estimatedScrollYProperty().setValue( yScroll );
							}
						} );
						
					} catch (Throwable e) {
						previewPane.replaceText( "-- Error Generating Example Output --");
						e.printStackTrace( System.out );
					} 
					
				} else {
					previewPane.replaceText( "" );
				}
				oldSelectedObject = selectedObject;
			}
		} );
	}
	
	/**
	 * Called when the user clicks the button to save the current example output to file.
	 * 
	 * @param event  the action event that triggered this method call
	 */
	@FXML public void saveExampleOutput(ActionEvent event) {
		boolean xmlSelected = xmlRadio.isSelected();
		UserSettings userSettings = UserSettings.load();
		FileChooser chooser = newFileChooser( "Save Example Output", userSettings.getLastExampleFolder(),
				xmlSelected ? new FileChooser.ExtensionFilter( "XML Files", "*.xml" )
							: new FileChooser.ExtensionFilter( "JSON Files", "*.json" ) );
		File targetFile = chooser.showSaveDialog( primaryStage );
		
		if (targetFile != null) {
			Runnable r = new BackgroundTask( "Saving Report" ) {
				protected void execute() throws Throwable {
					try {
						try (Writer out = new FileWriter( targetFile )) {
							out.write( previewPane.getText() );
						}
						
					} finally {
						userSettings.setLastExampleFolder( targetFile.getParentFile() );
						userSettings.save();
					}
				}
			};
			
			new Thread( r ).start();
		}
	}
	
	/**
	 * Updates the enabled/disables states of the visual controls based on the current
	 * state of user selections.
	 */
	private void updateControlStates() {
		Platform.runLater( new Runnable() {
			public void run() {
				libraryText.setText( (modelFile == null) ? "" : modelFile.getName() );
			}
		} );
	}
	
	/**
	 * Displays a message to the user in the status bar and optionally disables the
	 * interactive controls on the display.
	 * 
	 * @param message  the status bar message to display
	 * @param disableControls  flag indicating whether interactive controls should be disabled
	 */
	private void setStatusMessage(String message, boolean disableControls) {
		Platform.runLater( new Runnable() {
			public void run() {
				statusBarLabel.setText( message );
				libraryText.disableProperty().set( disableControls );
				bindingStyleChoice.disableProperty().set( disableControls );
				entityChoice.disableProperty().set( disableControls );
				repeatCountSpinner.disableProperty().set( disableControls );
				xmlRadio.disableProperty().set( disableControls );
				jsonRadio.disableProperty().set( disableControls );
				treeView.disableProperty().set( disableControls );
				saveButton.disableProperty().set( disableControls );
			}
		});
	}
	
	/**
	 * Assigns the primary stage for the window associated with this controller.
	 *
	 * @param primaryStage  the primary stage for this controller
	 * @param userSettings  provides user setting information from the last application session
	 */
	public void initialize(Stage primaryStage, UserSettings settings) {
		List<String> bindingStyles = CompilerExtensionRegistry.getAvailableExtensionIds();
		String defaultStyle = CompilerExtensionRegistry.getActiveExtension();
		
		// Since the preview pane is a custom component, we have to configure it manually
		previewPane = new CodeArea();
		previewPane.setEditable( false );
		previewScrollPane = new VirtualizedScrollPane<>( previewPane );
		Node pane = new StackPane( previewScrollPane );
		previewVBox.getChildren().add( pane );
		VBox.setVgrow(pane, Priority.ALWAYS);
		
		// Configure listeners to capture interactions with the visual controls
		bindingStyleChoice.setItems( FXCollections.observableArrayList( bindingStyles ) );
		bindingStyleChoice.setValue( defaultStyle );
		bindingStyleChoice.valueProperty().addListener( new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String>
						observable, String oldValue, String newValue) {
				handleBindingStyleChange();
			}
		} );
		
		repeatCountSpinner.setValueFactory(
				new IntegerSpinnerValueFactory( 1, 3, settings.getRepeatCount(), 1 ) );
		repeatCountSpinner.valueProperty().addListener( new ChangeListener<Integer>() {
			public void changed(ObservableValue<? extends Integer>
						observable, Integer oldValue, Integer newValue) {
				refreshExample();
			}
		} );
		
		entityChoice.valueProperty().addListener( new ChangeListener<OTMObjectChoice>() {
			public void changed(ObservableValue<? extends OTMObjectChoice>
						observable, OTMObjectChoice oldValue, OTMObjectChoice newValue) {
				entitySelectionChanged();
			}
		} );
		
		xmlRadio.selectedProperty().addListener( new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean>
						observable, Boolean oldValue, Boolean newValue) {
				refreshExample();
			}
		} );
		
		objectPropertyColumn.setCellValueFactory(new Callback<CellDataFeatures<EntityMemberNode,String>, ObservableValue<String>>() {
			public ObservableValue<String> call(CellDataFeatures<EntityMemberNode,String> nodeFeatures) {
				return new ReadOnlyObjectWrapper<String>( nodeFeatures.getValue().getValue().getName() );
			}
		});
		facetSelectionColumn.setCellValueFactory(new Callback<CellDataFeatures<EntityMemberNode,String>, ObservableValue<String>>() {
			public ObservableValue<String> call(CellDataFeatures<EntityMemberNode,String> nodeFeatures) {
				String selectedFacet = nodeFeatures.getValue().getValue().getFacetSelection().getSelectedFacetName();
				return (selectedFacet == null) ? null : new ReadOnlyObjectWrapper<String>( selectedFacet );
			}
		});
		facetSelectionColumn.setCellFactory(new Callback<TreeTableColumn<EntityMemberNode,String>,TreeTableCell<EntityMemberNode,String>>() {
			public TreeTableCell<EntityMemberNode,String> call(TreeTableColumn<EntityMemberNode,String> column) {
				return new FacetSelectCell();
			}
		});
		facetSelectionColumn.setEditable( true );
		treeView.setEditable( true );
		
		previewPane.textProperty().addListener( new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				saveButton.setDisable( (newValue == null) || (newValue.length() == 0) );
			}
		});
		
		saveButton.setDisable( true );
		
		this.primaryStage = primaryStage;
		primaryStage.getScene().getStylesheets().add(
				ExampleHelperController.class.getResource( "/styles/xml-highlighting.css" ).toExternalForm() );
		primaryStage.getScene().getStylesheets().add(
				ExampleHelperController.class.getResource( "/styles/json-highlighting.css" ).toExternalForm() );
	}
	
	/**
	 * Allows the controller to save any updates to the user settings prior to application
	 * close.
	 * 
	 * @param settings  the user settings to be updated
	 */
	public void updateUserSettings(UserSettings settings) {
		if (modelFile != null) {
			settings.setLastModelFile( modelFile );
		}
		if (exampleFolder != null) {
			settings.setLastExampleFolder( exampleFolder );
		}
		settings.setRepeatCount( repeatCountSpinner.getValue() );
	}
	
	/**
	 * Abstract class that executes a background task in a non-UI thread.
	 */
	private abstract class BackgroundTask implements Runnable {
		
		private String statusMessage;
		
		/**
		 * Constructor that specifies the status message to display during task execution.
		 * 
		 * @param statusMessage  the status message for the task
		 */
		public BackgroundTask(String statusMessage) {
			this.statusMessage = statusMessage;
		}
		
		/**
		 * Executes the sub-class specific task functions.
		 * 
		 * @throws Throwable  thrown if an error occurs during task execution
		 */
		protected abstract void execute() throws Throwable;

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				setStatusMessage( statusMessage, true );
				execute();
				
			} catch (Throwable t) {
				String errorMessage = (t.getMessage() != null) ? t.getMessage() : "See log output for details.";
				
				try {
					setStatusMessage( "ERROR: " + errorMessage, false );
					updateControlStates();
					t.printStackTrace( System.out );
					Thread.sleep( 1000 );
					
				} catch (InterruptedException e) {}
				
			} finally {
				setStatusMessage( null, false );
				updateControlStates();
			}
		}
		
	}
	
	/**
	 * Provides a list item for a choice-box that can be used to display and
	 * select OTM objects.
	 */
	private static class OTMObjectChoice {

		private NamedEntity otmObject;

		/**
		 * Constructor that provides the OTM object.
		 * 
		 * @param otmObject
		 *            the OTM object instance
		 */
		public OTMObjectChoice(NamedEntity otmObject) {
			this.otmObject = otmObject;
		}

		/**
		 * Returns the OTM object instance.
		 *
		 * @return NamedEntity
		 */
		public NamedEntity getOtmObject() {
			return otmObject;
		}

		/**
		 * Returns the display name for the OTM object in the combo-box.
		 *
		 * @return String
		 */
		public String toString() {
			return HelperUtils.getDisplayName( otmObject, true );
		}

	}
	
	/**
	 * Table cell that allows the user to select a facet from the available list
	 * for substitutable OTM entities.
	 */
	private class FacetSelectCell extends ChoiceBoxTreeTableCell<EntityMemberNode,String> {
		
		/**
		 * Default constructor.
		 */
		public FacetSelectCell() {
			setOnMouseEntered( new EventHandler<MouseEvent>() {
			    public void handle(MouseEvent me) {
			    	boolean isEditable = FacetSelectCell.this.isEditable();
			    	FacetSelectCell.this.getScene().setCursor( isEditable ? Cursor.HAND : Cursor.DEFAULT );
			    }
			} );
			setOnMouseExited( new EventHandler<MouseEvent>() {
			    public void handle(MouseEvent me) {
			    	FacetSelectCell.this.getScene().setCursor( Cursor.DEFAULT );
			    }
			} );
		}

		/**
		 * @see javafx.scene.control.TreeTableCell#commitEdit(java.lang.Object)
		 */
		@Override
		public void commitEdit(String value) {
			EntityMemberNode entityNode = getTreeTableRow().getItem();
			
			if (entityNode != null) {
				EntityFacetSelection facetSelection = entityNode.getFacetSelection();
				
				facetSelection.setSelectedFacet( value );
				
				// Refresh the contents of the tree
				Platform.runLater( new Runnable() {
					public void run() {
						fireTreeItemEvents( treeView.getRoot() );
					}
					
					private void fireTreeItemEvents(TreeItem<EntityMemberNode> treeItem) {
						Event.fireEvent( treeItem, new TreeModificationEvent<EntityMemberNode>(
								TreeItem.childrenModificationEvent(), treeItem ) );
						Event.fireEvent( treeItem, new TreeModificationEvent<EntityMemberNode>(
								TreeItem.valueChangedEvent(), treeItem ) );
						
						for (TreeItem<EntityMemberNode> childItem : treeItem.getChildren()) {
							fireTreeItemEvents( childItem );
						}
					}
				});
			}
			super.commitEdit( value );
		}

		/**
		 * @see javafx.scene.control.cell.ChoiceBoxTreeTableCell#updateItem(java.lang.Object, boolean)
		 */
		@Override
		public void updateItem(String value, boolean empty) {
			List<String> itemList = null;
			boolean editable = false;
			
			if (!empty) {
				EntityMemberNode entityNode = getTreeTableRow().getItem();
				itemList = (entityNode == null) ? null : entityNode.getFacetSelection().getFacetNames(); 
				
				editable = (itemList != null) && (itemList.size() > 1);
				if (!editable) itemList = null;
			}
			setEditable( editable );
			getItems().clear();
			
			if (editable) {
				getItems().addAll( FXCollections.observableArrayList( itemList ) );
				setStyle( "-fx-font-weight:bold;" );
				
			} else {
				setStyle( null );
			}
			super.updateItem( value, empty );
		}
		
	}
	
}
