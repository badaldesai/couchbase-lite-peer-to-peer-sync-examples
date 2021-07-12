//
//  EntryViewController.swift
//  list-sync
//
//  Created by Badal Desai on 2021-07-08.
//  Copyright © 2021 Couchbase Inc. All rights reserved.
//

import UIKit
import Foundation
import CouchbaseLiteSwift

class EntryViewController: UIViewController, UITextFieldDelegate, ListPresentingViewProtocol {
    func updateUIWithListRecord(_ record: ListRecord?, error: Error?) {    }
    
    
    @IBOutlet var field: UITextField!
    fileprivate var record:ListRecord?
    lazy var listPresenter:ListPresenter = ListPresenter()

    override func viewDidLoad() {
        super.viewDidLoad()
        field.delegate = self
        // Do any additional setup after loading the view.
        self.setupNavigationBar(title: NSLocalizedString(
                "Add new Fruit", comment:""))
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        return true
    }
}


extension EntryViewController {
    @IBAction func onSaveTapped(_ sender: UIBarButtonItem) {
        let type = "list"
        let dbMgr:DatabaseManager = DatabaseManager.shared
        let docId = "\(dbMgr.kDocPrefix)\(type)"
        guard let text = field.text, !text.isEmpty else {
            return
        }
        
        var listRecord = ListRecord.init(items: [])
        guard let db = dbMgr.userDB else {
            fatalError("db is not initialized at this point!")
        }
        if let doc = db.document(withID: docId)   {
            if let listItems = doc.array(forKey: ListDocumentKeys.items.rawValue)?.toArray()
                as? [[String:Any]]{
                for item in listItems {
                    let key =  item[ListItemDocumentKeys.key.rawValue] as? String
                    let value =  item[ListItemDocumentKeys.value.rawValue]
                    let image = item[ListItemDocumentKeys.image.rawValue] as? Blob
                    listRecord.items.append((image: image?.content, key: key, value: value))
                    
                }
            }
        }
       // var modifiedItem:[String:Any] = [:]
       
        let imageFile:String = "default_food"
        
        let defaultImage = UIImage.init(imageLiteralResourceName: imageFile)
        if let imageData = defaultImage.jpegData(compressionQuality: 0.75) {
            listRecord.items.append((image: imageData, key: text, value: 100))
        }
        
        self.listPresenter.setRecordOfType(kListRecordDocumentType,record:listRecord, handler: { [weak self](error) in
            guard let `self` = self else {
                return
            }
            if error != nil {
                self.showAlertWithTitle(NSLocalizedString("Error!", comment: ""), message: (error?.localizedDescription) ?? "Failed to update list record")
            }
            else {
                self.showAlertWithTitle("", message: "Succesfully updated List!")
            }
        })
        navigationController?.popViewController(animated: true)
    }
}

//MARK : Navigation Bar Setup
extension EntryViewController {
    override func setupNavigationBar(title: String) {
     
        super.setupNavigationBar(title: title)

       //show right button
       let rightButton = UIBarButtonItem(title: "Save", style: UIBarButtonItem.Style.plain, target: self, action: #selector(onSaveTapped(_:)))

       self.navigationItem.rightBarButtonItem = rightButton
   
   }
}
