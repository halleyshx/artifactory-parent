class ArtifactoryModelSaver {
    constructor(controller, modelObjects, excludePaths, $timeout, JFrogModal, $q, ArtifactoryState) {
        this.JFrogModal = JFrogModal;
        this.$q = $q;

        this.controller = controller;
        this.controller._$modelSaver$_ = this;
        this.confirmOnLeave = true;
        this.modelObjects = modelObjects;
        this.excludePaths = excludePaths;
        this.savedModels = {};
        this.saved = false;
        this.artifactoryState = ArtifactoryState;

        $timeout(()=>{
            if (!this.saved) this.save();
        })
    }

    save() {
        this.modelObjects.forEach((objName)=>{
            this.savedModels[objName] = _.cloneDeep(this.controller[objName]);
        });
        this.saved = true;
    }

    isModelSaved() {
        let isSaved = true;
        for (let objectNameI in this.modelObjects) {
            let objectName = this.modelObjects[objectNameI];
            if (!angular.equals(this.savedModels[objectName],this.controller[objectName])) {
                let deefObj = DeepDiff(this.savedModels[objectName],this.controller[objectName]);
//                console.log(deefObj);
                if (this._isDiffReal(deefObj,this.excludePaths[objectNameI])) {
                    isSaved = false;
                    break;
                }
            }
        }
        return isSaved;
    }


    _isDiffReal(deefObj,excludePaths) {

        let excludes = excludePaths ? excludePaths.split(';') : [];

        let isReal = false;

        for (let key in deefObj) {
            let deef = deefObj[key];

            if (deef.path && deef.path.length && ((!_.isString(deef.path[deef.path.length-1]) || deef.path[deef.path.length-1].startsWith('$$')) || this._isExcluded(deef.path,excludes))) continue;

            if ((deef.lhs === undefined && deef.rhs === '') || (deef.lhs === '' && deef.rhs === undefined) ||
                (deef.lhs === undefined && _.isArray(deef.rhs) && deef.rhs.length === 0) ||
                (deef.lhs === undefined && _.isObject(deef.rhs) && Object.keys(deef.rhs).length === 0)) {
                // not real
            }
            else { //real
                isReal = true;
                break;
            }
        }

        return isReal;

    }

    _isExcluded(path,excludes) {
        if (!excludes.length) return false;
        let excluded = false;
        for (let i in excludes) {
            let exclude = excludes[i];
            let exPath = exclude.split('.');
            let match = true;
            for (let pI in exPath) {
                if ((exPath[pI] !== '*' && exPath[pI] !== path[pI]) || (exPath[pI] === '*' && path[pI]) === undefined) {
                    match = false;
                    break;
                }
            }
            if (match) excluded = true;
            break;
        }

        return excluded;
    }


    ask(reset = false) {
        let message = reset ? 'You have unsaved changes. Reset action will discard changes.' : 'You have unsaved changes. Leaving this page will discard changes.';
        let defer = this.$q.defer();

        if (!this.isModelSaved()) {
            this.JFrogModal.confirm(message, 'Discard Changes', {confirm: 'Discard'})
                    .then(()=>{
                        defer.resolve();
                        this.artifactoryState.setState('confirmDiscardModalOpen',false);
                    }).catch(()=>this.artifactoryState.setState('confirmDiscardModalOpen',false));

            this.artifactoryState.setState('confirmDiscardModalOpen',true);
        }
        else {
            defer.resolve();
        }
        return defer.promise;
    }
}

export function ArtifactoryModelSaverFactory($timeout, JFrogModal, $q, ArtifactoryState) {
    return {
        createInstance: (controller,modelObjects,excludePaths) => {
            excludePaths = excludePaths || [];
    return new ArtifactoryModelSaver(controller, modelObjects, excludePaths, $timeout, JFrogModal, $q, ArtifactoryState);
        }
    }
}