//
//  CustomMarker.swift
//  Runner
//
//  Created by SeungHa Park on 2022/08/05.
//

import UIKit

class CustomMarker: UIView {

    /*
    // Only override draw() if you perform custom drawing.
    // An empty implementation adversely affects performance during animation.
    override func draw(_ rect: CGRect) {
        // Drawing code
    }
    */
    
    
    let imageView: UIImageView = {
          let view = UIImageView()
          view.translatesAutoresizingMaskIntoConstraints = false
          view.image = UIImage(named: "markerBack")
          return view
      }()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupView()
    }
    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupView()
    }
    
    
       func setupView() {
           backgroundColor = .orange
           addSubview(imageView)
   
           
           NSLayoutConstraint.activate([
            imageView.widthAnchor.constraint(equalToConstant: self.frame.width),
            imageView.heightAnchor.constraint(equalToConstant: self.frame.height),

           ])
       }
    func addMarkerImage(image: UIImage){
        let markerImage : UIImageView = {
            let view = UIImageView()
            view.translatesAutoresizingMaskIntoConstraints = false
            view.image = image
            return view
        }()
        addSubview(markerImage)
        NSLayoutConstraint.activate([
            markerImage.centerXAnchor.constraint(equalTo: imageView.centerXAnchor),
            markerImage.widthAnchor.constraint(equalToConstant: 40),
            markerImage.heightAnchor.constraint(equalToConstant: 40),
            markerImage.topAnchor.constraint(equalTo: imageView.topAnchor,constant: 5),
         
            
        ])
    }
}
