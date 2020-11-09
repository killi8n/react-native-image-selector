//
//  ImageShowerCell.swift
//  ImageSelector
//
//  Created by Dongho Choi on 2020/11/04.
//  Copyright © 2020 Facebook. All rights reserved.
//

import UIKit
import Photos

class ImageShowerCell: UICollectionViewCell {
    let cellImageView: UIImageView = {
        let imageView = UIImageView(frame: .zero)
        imageView.contentMode = .scaleAspectFill
        imageView.layer.masksToBounds = true
        return imageView
    }()
    override init(frame: CGRect) {
        super.init(frame: frame)
        self.contentView.backgroundColor = .groupTableViewBackground
        self.contentView.addSubview(self.cellImageView)
        self.cellImageView.translatesAutoresizingMaskIntoConstraints = false
        let topAnchor = self.cellImageView.topAnchor.constraint(equalTo: self.contentView.topAnchor)
        let leftAnchor = self.cellImageView.leftAnchor.constraint(equalTo: self.contentView.leftAnchor)
        let bottomAnchor = self.cellImageView.bottomAnchor.constraint(equalTo: self.contentView.bottomAnchor)
        let rightAnchor = self.cellImageView.rightAnchor.constraint(equalTo: self.contentView.rightAnchor)
        self.contentView.addConstraints([topAnchor, leftAnchor, bottomAnchor, rightAnchor])
    }

    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func prepareForReuse() {
        self.cellImageView.image = nil
    }
    
    func fetchImage(asset: PHAsset) -> Void {
        let cellWidth = self.contentView.frame.size.width
        let cellHeight = self.contentView.frame.size.height
        ImageUtil.requestImage(asset: asset, deliveryMode: .opportunistic, resizeMode: .fast, targetSize: CGSize(width: cellWidth + cellWidth / 2, height: cellHeight + cellHeight / 2), contentMode: .aspectFill) { [weak self] (image: UIImage) in
            guard let `self` = self else { return }
            self.cellImageView.image = image
        } exceptionCompletion: {
            
        }
    }
}

