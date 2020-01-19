/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function($) {
    BS.CodeBuild = {
        updateArtifactsSettingsVisibility: function () {
            var showArtifactsSettings = $('#runnerParams #codebuild_artifacts option:selected').val() == 's3';
            $('#runnerParams .artifactsSetting').each(function () {
                if (showArtifactsSettings) BS.Util.show(this);
                else BS.Util.hide(this);
            });

            BS.VisibilityHandlers.updateVisibility('runnerParams');
        },
        updateArtifactsName: function () {
            if ($('#codebuild_artifacts_s3_zip').is(':checked')) {
                BS.Util.show('noteArchive');
                BS.Util.hide('noteFolder');
            } else {
                BS.Util.hide('noteArchive');
                BS.Util.show('noteFolder');
            }
            BS.VisibilityHandlers.updateVisibility('runnerParams');
        },
        updateSourceVersionVisibility: function (sourceType) {
            // $('#runnerParams .sourceTypeSetting').each(function () {
            //     if (sourceType) {
            //         Form.Element.disable(this);
            //     } else {
            //         Form.Element.enable(this);
            //     }
            // });
            // if (sourceType) {
            //     $('#runnerParams .' + sourceType).each(function () {
            //         Form.Element.enable(this);
            //     });
            // }
            // BS.VisibilityHandlers.updateVisibility('runnerParams');
        }
    };

    BS.CodeBuildFakeForm = OO.extend(BS.PluginPropertiesForm, {
        formElement: function() {
            return $('#editBuildTypeForm').get(0);
        }
    });

    BS.CodeBuildProjectNamePopup = new BS.Popup("codeBuildProjectNamePopup", {
        url: window['base_uri'] + "/plugins/aws-codebuild-plugin/listProjects.html",
        method: "get",
        hideOnMouseOut: false,
        hideOnMouseClickOutside: true,
        shift: {x: 0, y: 20}
    });

    BS.CodeBuildProjectNamePopup.showPopup = function(nearestElement) {
        this.options.parameters = BS.CodeBuildFakeForm.serializeParameters();
        BS.CodeBuildFakeForm.clearErrors();
        this.showPopupNearElement(nearestElement);
    };

    BS.CodeBuildProjectNamePopup.fillProjectName = function(name, sourceType) {
        $('#codebuild_project_name').val(name);
        BS.CodeBuild.updateSourceVersionVisibility(sourceType);
        BS.CodeBuildProjectNamePopup.hidePopup(0);
    };

    $(document).ready(function() {
        BS.CodeBuild.updateArtifactsSettingsVisibility();
        BS.CodeBuild.updateSourceVersionVisibility();
        BS.CodeBuild.updateArtifactsName();
    });
})(jQuery);
